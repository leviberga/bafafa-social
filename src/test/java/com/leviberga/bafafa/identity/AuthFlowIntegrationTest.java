package com.leviberga.bafafa.identity;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthFlowIntegrationTest {

    private static final String COOKIE = "bafafa_refresh";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    MockMvc mvc;

    @Test
    void registerLoginAndMe() throws Exception {
        String id = newId();
        register(id + "@exemplo.com", id).andExpect(status().isCreated());

        MvcResult login = login(id, "senha-forte-123").andExpect(status().isOk()).andReturn();
        String accessToken = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handle").value(id));

        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        String id = newId();
        register(id + "@exemplo.com", id).andExpect(status().isCreated());

        login(id, "senha-errada-123").andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailIsConflict() throws Exception {
        String id = newId();
        register(id + "@exemplo.com", id).andExpect(status().isCreated());

        register(id + "@exemplo.com", id + "x").andExpect(status().isConflict());
    }

    @Test
    void refreshRotatesAndReuseRevokesWholeFamily() throws Exception {
        String id = newId();
        register(id + "@exemplo.com", id).andExpect(status().isCreated());

        String a = refreshCookie(login(id, "senha-forte-123").andExpect(status().isOk()).andReturn());

        String b = refreshCookie(refresh(a).andExpect(status().isOk()).andReturn());
        assertNotEquals(a, b);

        refresh(a).andExpect(status().isUnauthorized()); // reuso do token antigo
        refresh(b).andExpect(status().isUnauthorized()); // a família inteira foi revogada
    }

    @Test
    void logoutRevokesSession() throws Exception {
        String id = newId();
        register(id + "@exemplo.com", id).andExpect(status().isCreated());

        String a = refreshCookie(login(id, "senha-forte-123").andExpect(status().isOk()).andReturn());

        mvc.perform(post("/api/v1/auth/logout").cookie(new Cookie(COOKIE, a)))
                .andExpect(status().isNoContent());
        refresh(a).andExpect(status().isUnauthorized());
    }

    private static String newId() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private ResultActions register(String email, String handle) throws Exception {
        return mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"senha-forte-123","handle":"%s","displayName":"Teste"}
                        """.formatted(email, handle)));
    }

    private ResultActions login(String identifier, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"identifier":"%s","password":"%s"}
                        """.formatted(identifier, password)));
    }

    private ResultActions refresh(String cookieValue) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(COOKIE, cookieValue)));
    }

    private static String refreshCookie(MvcResult result) {
        String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return header.substring(header.indexOf('=') + 1, header.indexOf(';'));
    }
}