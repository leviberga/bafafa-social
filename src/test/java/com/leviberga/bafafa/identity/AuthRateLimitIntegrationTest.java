package com.leviberga.bafafa.identity;

import com.leviberga.bafafa.TestClient;
import com.leviberga.bafafa.TestcontainersConfiguration;
import com.leviberga.bafafa.identity.application.AuthRateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthRateLimitIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthRateLimitProperties limits;

    @Test
    void registrationIsLimitedPerIp() throws Exception {
        RequestPostProcessor sameIp = TestClient.newIp();

        for (int i = 0; i < limits.registerPerIp().capacity(); i++) {
            register(newId(), sameIp).andExpect(status().isCreated());
        }

        register(newId(), sameIp)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));

        register(newId(), TestClient.newIp()).andExpect(status().isCreated());
    }

    @Test
    void loginIsLimitedPerIpAcrossDifferentAccounts() throws Exception {
        RequestPostProcessor sameIp = TestClient.newIp();

        for (int i = 0; i < limits.loginPerIp().capacity(); i++) {
            login(newId(), sameIp).andExpect(status().isUnauthorized());
        }

        login(newId(), sameIp)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void loginIsLimitedPerAccountEvenFromDifferentIps() throws Exception {
        String victim = newId();

        for (int i = 0; i < limits.loginPerAccount().capacity(); i++) {
            login(victim, TestClient.newIp()).andExpect(status().isUnauthorized());
        }

        login(victim, TestClient.newIp())
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    private static String newId() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private ResultActions register(String id, RequestPostProcessor ip) throws Exception {
        return mvc.perform(post("/api/v1/auth/register")
                .with(ip)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s@exemplo.com","password":"senha-forte-123","handle":"%s","displayName":"Teste"}
                        """.formatted(id, id)));
    }

    private ResultActions login(String identifier, RequestPostProcessor ip) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .with(ip)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"identifier":"%s","password":"senha-errada-123"}
                        """.formatted(identifier)));
    }
}