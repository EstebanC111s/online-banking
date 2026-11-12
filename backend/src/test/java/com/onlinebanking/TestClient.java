package com.onlinebanking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TestClient {

    private final MockMvc mvc;
    private final ObjectMapper json;

    TestClient(MockMvc mvc, ObjectMapper json) {
        this.mvc = mvc;
        this.json = json;
    }

    static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.co";
    }

    Map<String, Object> registerBody(String name, String email, LocalDate birth) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fullName", name);
        body.put("email", email);
        body.put("password", "Clave1234");
        body.put("birthDate", birth.toString());
        body.put("acceptTerms", true);
        body.put("acceptPrivacy", true);
        return body;
    }

    JsonNode registerAdult(String name, String email) throws Exception {
        return call(post("/api/auth/register"), null, registerBody(name, email, LocalDate.now().minusYears(30)), 201);
    }

    String login(String email, String password) throws Exception {
        return call(post("/api/auth/login"), null, Map.of("email", email, "password", password), 200).get("token").asText();
    }

    String newCustomer(String name) throws Exception {
        String email = uniqueEmail(name.toLowerCase().replace(' ', '.'));
        registerAdult(name, email);
        return login(email, "Clave1234");
    }

    JsonNode openAccount(String token) throws Exception {
        return call(post("/api/accounts"), token, Map.of("type", "SAVINGS"), 201);
    }

    void deposit(String token, long accountId, String amount) throws Exception {
        call(post("/api/accounts/" + accountId + "/deposits"), token, Map.of("amount", amount), 201);
    }

    JsonNode call(MockHttpServletRequestBuilder req, String token, Object body, int expectedStatus) throws Exception {
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        MvcResult res = mvc.perform(req).andExpect(status().is(expectedStatus)).andReturn();
        String content = res.getResponse().getContentAsString();
        return content.isBlank() ? json.nullNode() : json.readTree(content);
    }

    JsonNode get(String url, String token, int expectedStatus) throws Exception {
        return call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url), token, null, expectedStatus);
    }

    String lastTokenSentTo(String recipient, String linkPath) throws Exception {
        JsonNode outbox = get("/api/dev/outbox", null, 200);
        Pattern p = Pattern.compile(Pattern.quote(linkPath) + "\\?token=([A-Za-z0-9_-]+)");
        for (JsonNode m : outbox) {
            if (m.get("recipient").asText().equalsIgnoreCase(recipient)) {
                Matcher matcher = p.matcher(m.get("body").asText());
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        throw new AssertionError("No se encontró enlace " + linkPath + " para " + recipient);
    }
}
