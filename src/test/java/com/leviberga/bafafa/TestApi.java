package com.leviberga.bafafa;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class TestApi {

    private TestApi() {
    }

    public record User(String handle, String token) {

        public String bearer() {
            return "Bearer " + token;
        }
    }

    public static User newUser(MockMvc mvc) throws Exception {
        String id = "user_" + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/api/v1/auth/register")
                        .with(TestClient.newIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s@exemplo.com","password":"senha-forte-123","handle":"%s","displayName":"Teste"}
                                """.formatted(id, id)))
                .andExpect(status().isCreated());

        String body = mvc.perform(post("/api/v1/auth/login")
                        .with(TestClient.newIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"senha-forte-123"}
                                """.formatted(id)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new User(id, JsonPath.read(body, "$.accessToken"));
    }

    public static String createPost(MockMvc mvc, User user, String text) throws Exception {
        String body = mvc.perform(post("/api/v1/posts")
                        .header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"%s"}
                                """.formatted(text)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}