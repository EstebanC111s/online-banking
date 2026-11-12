package com.onlinebanking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
class ComplianceFeaturesTest {

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
    void publicTransparencyPagesAreAvailableWithoutLogin() throws Exception {
        for (String slug : new String[]{"terminos", "privacidad", "cookies", "devoluciones", "ia"}) {
            assertThat(api.get("/api/public/legal/" + slug, null, 200).get("sections").size()).isPositive();
        }
        assertThat(api.get("/api/public/company", null, 200).get("disclaimer").asText()).contains("académico");
        assertThat(api.get("/api/public/fees", null, 200).size()).isPositive();
        assertThat(api.get("/api/public/licenses", null, 200).size()).isPositive();
        assertThat(api.get("/api/public/integrations", null, 200).size()).isPositive();
        assertThat(api.get("/api/public/promises", null, 200)).allSatisfy(p -> assertThat(p.has("evidence")).isTrue());
    }

    @Test
    void registrationRequiresAcceptingTermsAndPrivacy() throws Exception {
        Map<String, Object> body = api.registerBody("Sin Aceptar", TestClient.uniqueEmail("sin"), LocalDate.now().minusYears(25));
        body.put("acceptTerms", false);
        JsonNode error = api.call(post("/api/auth/register"), null, body, 400);
        assertThat(error.get("fields").has("acceptTerms")).isTrue();
    }

    @Test
    void consentsDefaultToNoAndCanBeChanged() throws Exception {
        String token = api.newCustomer("Carla Consent");
        JsonNode consents = api.get("/api/me/consents", token, 200);
        assertThat(consents).allSatisfy(c -> assertThat(c.get("granted").asBoolean()).isFalse());

        api.call(put("/api/me/consents/ANALYTICS"), token, Map.of("granted", true), 200);
        JsonNode history = api.get("/api/me/consents/history", token, 200);
        assertThat(history.get(0).get("type").asText()).isEqualTo("ANALYTICS");
        assertThat(history.get(0).get("granted").asBoolean()).isTrue();
    }

    @Test
    void minorsNeedGuardianApprovalBeforeLoggingIn() throws Exception {
        String email = TestClient.uniqueEmail("menor");
        Map<String, Object> body = api.registerBody("Mateo Menor", email, LocalDate.now().minusYears(15));

        JsonNode error = api.call(post("/api/auth/register"), null, body, 400);
        assertThat(error.get("code").asText()).isEqualTo("GUARDIAN_REQUIRED");

        String guardian = TestClient.uniqueEmail("tutor");
        body.put("guardianEmail", guardian);
        assertThat(api.call(post("/api/auth/register"), null, body, 201).get("status").asText()).isEqualTo("PENDING_GUARDIAN");

        JsonNode blocked = api.call(post("/api/auth/login"), null, Map.of("email", email, "password", "Clave1234"), 403);
        assertThat(blocked.get("code").asText()).isEqualTo("PENDING_GUARDIAN");

        String token = api.lastTokenSentTo(guardian, "/tutor");
        api.call(post("/api/public/guardian/approve"), null, Map.of("token", token), 200);
        assertThat(api.login(email, "Clave1234")).isNotBlank();

        api.call(post("/api/public/guardian/approve"), null, Map.of("token", token), 400);
    }

    @Test
    void underMinimumAgeIsRejected() throws Exception {
        Map<String, Object> body = api.registerBody("Muy Pequeño", TestClient.uniqueEmail("nino"), LocalDate.now().minusYears(10));
        body.put("guardianEmail", TestClient.uniqueEmail("papa"));
        assertThat(api.call(post("/api/auth/register"), null, body, 400).get("code").asText()).isEqualTo("TOO_YOUNG");
    }

    @Test
    void marketingOnlyWithConsentAndUnsubscribeLinkWorksWithoutLogin() throws Exception {
        String email = TestClient.uniqueEmail("promo");
        Map<String, Object> body = api.registerBody("Paula Promo", email, LocalDate.now().minusYears(28));
        body.put("marketingOptIn", true);
        api.call(post("/api/auth/register"), null, body, 201);

        String admin = api.login("admin@ceiba.demo", "Admin123!");
        JsonNode result = api.call(post("/api/admin/newsletter"), admin, Map.of("subject", "Novedades", "body", "Hola"), 200);
        assertThat(result.get("recipients").asInt()).isGreaterThanOrEqualTo(1);

        String token = api.lastTokenSentTo(email, "/baja");
        api.call(post("/api/public/unsubscribe"), null, Map.of("token", token), 200);

        String session = api.login(email, "Clave1234");
        JsonNode marketing = api.get("/api/me/consents", session, 200).get(0);
        assertThat(marketing.get("type").asText()).isEqualTo("MARKETING");
        assertThat(marketing.get("granted").asBoolean()).isFalse();
        assertThat(marketing.get("source").asText()).isEqualTo("UNSUBSCRIBE_LINK");
    }

    @Test
    void adminEndpointsAreForbiddenForCustomers() throws Exception {
        String token = api.newCustomer("Cliente Normal");
        api.get("/api/admin/disputes", token, 403);
    }

    @Test
    void newAccountCanBeRetractedOnlyWithZeroBalance() throws Exception {
        String token = api.newCustomer("Rita Retracto");
        JsonNode acc = api.openAccount(token);
        assertThat(acc.get("canRetract").asBoolean()).isTrue();
        long id = acc.get("id").asLong();

        api.deposit(token, id, "5000");
        assertThat(api.call(post("/api/accounts/" + id + "/retract"), token, null, 409).get("code").asText())
                .isEqualTo("BALANCE_NOT_ZERO");

        long other = api.openAccount(token).get("id").asLong();
        String otherNumber = findNumber(token, other);
        JsonNode q = api.call(post("/api/transfers/quotes"), token,
                Map.of("fromAccountId", id, "toAccountNumber", otherNumber, "amount", "5000"), 201);
        api.call(post("/api/transfers/quotes/" + q.get("quoteId").asText() + "/confirm"), token, null, 200);

        assertThat(api.call(post("/api/accounts/" + id + "/retract"), token, null, 200).get("status").asText())
                .isEqualTo("RETRACTED");
    }

    @Test
    void disputedFeeIsRefundedAutomaticallyAndRejectionsNeedAReason() throws Exception {
        String ana = api.newCustomer("Ana Reclamo");
        String beto = api.newCustomer("Beto Destino");
        long acc = api.openAccount(ana).get("id").asLong();
        String betoNumber = api.openAccount(beto).get("number").asText();
        api.deposit(ana, acc, "30000");
        JsonNode q = api.call(post("/api/transfers/quotes"), ana,
                Map.of("fromAccountId", acc, "toAccountNumber", betoNumber, "amount", "10000"), 201);
        api.call(post("/api/transfers/quotes/" + q.get("quoteId").asText() + "/confirm"), ana, null, 200);

        JsonNode movements = api.get("/api/accounts/" + acc + "/transactions", ana, 200);
        long feeId = movements.get(0).get("id").asLong();
        long transferId = movements.get(1).get("id").asLong();

        JsonNode feeDispute = api.call(post("/api/transactions/" + feeId + "/disputes"), ana,
                Map.of("reason", "No esperaba esta comisión"), 201);
        assertThat(feeDispute.get("status").asText()).isEqualTo("REFUNDED");
        assertThat(api.get("/api/accounts", ana, 200).get(0).get("balance").decimalValue()).isEqualByComparingTo("20000");

        JsonNode transferDispute = api.call(post("/api/transactions/" + transferId + "/disputes"), ana,
                Map.of("reason", "Me equivoqué de destinatario"), 201);
        assertThat(transferDispute.get("status").asText()).isEqualTo("OPEN");
        assertThat(transferDispute.has("dueDate")).isTrue();

        String admin = api.login("admin@ceiba.demo", "Admin123!");
        long disputeId = transferDispute.get("id").asLong();
        api.call(post("/api/admin/disputes/" + disputeId + "/resolve"), admin, Map.of("refund", false), 409);
        JsonNode rejected = api.call(post("/api/admin/disputes/" + disputeId + "/resolve"), admin,
                Map.of("refund", false, "note", "La transferencia fue autorizada con tu confirmación"), 200);
        assertThat(rejected.get("status").asText()).isEqualTo("REJECTED");
        assertThat(rejected.get("resolutionNote").asText()).isNotBlank();
    }

    @Test
    void onlyCustomersWithActivityCanReview() throws Exception {
        String token = api.newCustomer("Rosa Reseña");
        Map<String, Object> review = Map.of("rating", 2, "comment", "La app es lenta a veces");
        assertThat(api.call(put("/api/reviews/mine"), token, review, 403).get("code").asText()).isEqualTo("NOT_VERIFIED");

        long acc = api.openAccount(token).get("id").asLong();
        api.deposit(token, acc, "1000");
        JsonNode saved = api.call(put("/api/reviews/mine"), token, review, 200);
        assertThat(saved.get("authorName").asText()).isEqualTo("Rosa R.");

        JsonNode summary = api.get("/api/public/reviews", null, 200);
        assertThat(summary.get("reviews")).anySatisfy(r -> assertThat(r.get("rating").asInt()).isEqualTo(2));
    }

    @Test
    void customerCanExportAndDeleteTheirData() throws Exception {
        String email = TestClient.uniqueEmail("borrar");
        api.registerAdult("Diego Borrar", email);
        String token = api.login(email, "Clave1234");
        long acc = api.openAccount(token).get("id").asLong();
        api.deposit(token, acc, "2500");

        JsonNode export = api.get("/api/me/export", token, 200);
        assertThat(export.get("profile").get("email").asText()).isEqualTo(email);
        assertThat(export.get("transactions").size()).isEqualTo(1);

        assertThat(api.call(post("/api/me/delete"), token, Map.of("password", "Clave1234"), 409).get("code").asText())
                .isEqualTo("BALANCE_NOT_ZERO");

        String other = api.newCustomer("Receptor Final");
        String otherNumber = api.openAccount(other).get("number").asText();
        JsonNode q = api.call(post("/api/transfers/quotes"), token,
                Map.of("fromAccountId", acc, "toAccountNumber", otherNumber, "amount", "1000"), 201);
        api.call(post("/api/transfers/quotes/" + q.get("quoteId").asText() + "/confirm"), token, null, 200);

        api.call(post("/api/me/delete"), token, Map.of("password", "otraClave1"), 401);
        api.call(post("/api/me/delete"), token, Map.of("password", "Clave1234"), 204);

        api.call(post("/api/auth/login"), null, Map.of("email", email, "password", "Clave1234"), 401);
        api.get("/api/me", token, 401);
    }

    private String findNumber(String token, long accountId) throws Exception {
        for (JsonNode a : api.get("/api/accounts", token, 200)) {
            if (a.get("id").asLong() == accountId) {
                return a.get("number").asText();
            }
        }
        throw new AssertionError();
    }
}
