package com.onlinebanking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties = "app.quote-ttl-seconds=0")
@AutoConfigureMockMvc
class QuoteExpiryTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    @Test
    void expiredQuoteCannotBeConfirmed() throws Exception {
        TestClient api = new TestClient(mvc, json);
        String ana = api.newCustomer("Ana Lenta");
        String beto = api.newCustomer("Beto Espera");
        long acc = api.openAccount(ana).get("id").asLong();
        String betoNumber = api.openAccount(beto).get("number").asText();
        api.deposit(ana, acc, "10000");

        JsonNode q = api.call(post("/api/transfers/quotes"), ana,
                Map.of("fromAccountId", acc, "toAccountNumber", betoNumber, "amount", "1000"), 201);
        Thread.sleep(5);

        JsonNode error = api.call(post("/api/transfers/quotes/" + q.get("quoteId").asText() + "/confirm"), ana, null, 410);
        assertThat(error.get("code").asText()).isEqualTo("QUOTE_EXPIRED");
        assertThat(api.get("/api/accounts", ana, 200).get(0).get("balance").decimalValue()).isEqualByComparingTo("10000");
    }
}
