package com.leviberga.bafafa.profile;

import com.jayway.jsonpath.JsonPath;
import com.leviberga.bafafa.TestClient;
import com.leviberga.bafafa.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(TestcontainersConfiguration.class)
class ProfileFlowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void profileIsCreatedFromRegistrationEventAndCanBeUpdated() throws Exception {
        String id = newId();
        register(id).andExpect(status().isCreated());

        // O perfil nasce de forma assíncrona, via evento: aguardamos ele aparecer.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mvc.perform(get("/api/v1/profiles/" + id))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.handle").value(id))
                        .andExpect(jsonPath("$.displayName").value("Teste"))
                        .andExpect(jsonPath("$.bio").value("")));

        String token = accessToken(id);

        mvc.perform(put("/api/v1/profiles/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Levi Berga","bio":"Dev backend"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Levi Berga"));

        mvc.perform(get("/api/v1/profiles/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Levi Berga"))
                .andExpect(jsonPath("$.bio").value("Dev backend"));
    }

    @Test
    void unknownHandleIsNotFound() throws Exception {
        mvc.perform(get("/api/v1/profiles/nao_existe_123")).andExpect(status().isNotFound());
    }

    @Test
    void myProfileRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/profiles/me")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/profiles/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"X","bio":""}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private static String newId() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private ResultActions register(String id) throws Exception {
        return mvc.perform(post("/api/v1/auth/register")
                .with(TestClient.newIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s@exemplo.com","password":"senha-forte-123","handle":"%s","displayName":"Teste"}
                        """.formatted(id, id)));
    }

    private String accessToken(String id) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login")
                        .with(TestClient.newIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"senha-forte-123"}
                                """.formatted(id)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}