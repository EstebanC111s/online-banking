package com.onlinebanking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class BankingFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    TestClient api;

    @BeforeEach
    void setUp() {
        api = new TestClient(mvc, json);
    }

    @Test
    void transferToAnotherPersonShowsFeeBeforeConfirmingAndIsIdempotent() throws Exception {
        String ana = api.newCustomer("Ana Gómez");
        String beto = api.newCustomer("Beto Ruiz");
        long anaAcc = api.openAccount(ana).get("id").asLong();
        JsonNode betoAccount = api.openAccount(beto);
        api.deposit(ana, anaAcc, "100000");

        JsonNode quote = api.call(post("/api/transfers/quotes"), ana, Map.of(
                "fromAccountId", anaAcc, "toAccountNumber", betoAccount.get("number").asText(),
                "amount", "50000", "description", "Arriendo"), 201);

        assertThat(quote.get("fee").decimalValue()).isEqualByComparingTo("1500");
        assertThat(quote.get("total").decimalValue()).isEqualByComparingTo("51500");
        assertThat(quote.get("ownAccount").asBoolean()).isFalse();
        assertThat(quote.get("recipientName").asText()).isEqualTo("Be*** Ru***");

        assertThat(balanceOf(ana, anaAcc)).isEqualByComparingTo("100000");

        String quoteId = quote.get("quoteId").asText();
        JsonNode first = api.call(post("/api/transfers/quotes/" + quoteId + "/confirm"), ana, null, 200);
        JsonNode second = api.call(post("/api/transfers/quotes/" + quoteId + "/confirm"), ana, null, 200);

        assertThat(first.get("reference").asText()).isEqualTo(second.get("reference").asText());
        assertThat(balanceOf(ana, anaAcc)).isEqualByComparingTo("48500");
        assertThat(balanceOf(beto, betoAccount.get("id").asLong())).isEqualByComparingTo("50000");

        JsonNode movements = api.get("/api/accounts/" + anaAcc + "/transactions", ana, 200);
        assertThat(movements).extracting(m -> m.get("type").asText())
                .containsExactly("FEE", "TRANSFER_OUT", "DEPOSIT");
    }

    @Test
    void transferBetweenOwnAccountsIsFree() throws Exception {
        String ana = api.newCustomer("Ana Propia");
        long a1 = api.openAccount(ana).get("id").asLong();
        JsonNode a2 = api.openAccount(ana);
        api.deposit(ana, a1, "20000");

        JsonNode quote = api.call(post("/api/transfers/quotes"), ana, Map.of(
                "fromAccountId", a1, "toAccountNumber", a2.get("number").asText(), "amount", "20000"), 201);

        assertThat(quote.get("fee").decimalValue()).isEqualByComparingTo("0");
        assertThat(quote.get("ownAccount").asBoolean()).isTrue();
    }

    @Test
    void insufficientFundsIsRejectedWithClearMessage() throws Exception {
        String ana = api.newCustomer("Ana Pobre");
        String beto = api.newCustomer("Beto Rico");
        long anaAcc = api.openAccount(ana).get("id").asLong();
        String betoNumber = api.openAccount(beto).get("number").asText();
        api.deposit(ana, anaAcc, "10000");

        JsonNode error = api.call(post("/api/transfers/quotes"), ana, Map.of(
                "fromAccountId", anaAcc, "toAccountNumber", betoNumber, "amount", "9000"), 409);

        assertThat(error.get("code").asText()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    void customersCannotSeeEachOthersAccounts() throws Exception {
        String ana = api.newCustomer("Ana Privada");
        String intruso = api.newCustomer("Intruso Curioso");
        long anaAcc = api.openAccount(ana).get("id").asLong();

        api.get("/api/accounts/" + anaAcc + "/transactions", intruso, 404);
        api.call(post("/api/accounts/" + anaAcc + "/deposits"), intruso, Map.of("amount", "5000"), 404);
    }

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        api.get("/api/accounts", null, 401);
    }

    @Test
    void invalidAmountsFailValidation() throws Exception {
        String ana = api.newCustomer("Ana Validada");
        long acc = api.openAccount(ana).get("id").asLong();
        JsonNode error = api.call(post("/api/accounts/" + acc + "/deposits"), ana, Map.of("amount", "-5"), 400);
        assertThat(error.get("code").asText()).isEqualTo("VALIDATION_ERROR");
    }

    private BigDecimal balanceOf(String token, long accountId) throws Exception {
        for (JsonNode a : api.get("/api/accounts", token, 200)) {
            if (a.get("id").asLong() == accountId) {
                return a.get("balance").decimalValue();
            }
        }
        throw new AssertionError("cuenta no encontrada");
    }
}
