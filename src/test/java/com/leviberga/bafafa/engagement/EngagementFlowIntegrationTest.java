package com.leviberga.bafafa.engagement;

import com.jayway.jsonpath.JsonPath;
import com.leviberga.bafafa.TestClient;
import com.leviberga.bafafa.TestcontainersConfiguration;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EngagementFlowIntegrationTest {

    @Autowired
    MockMvc mvc;

    private record User(String handle, String token) {
    }

    @Test
    void likeIsIdempotentAndReflectedInTheCount() throws Exception {
        User author = newUser();
        User fan = newUser();
        User other = newUser();
        String postId = createPost(author, "curte aí");

        like(fan, postId).andExpect(status().isNoContent());
        like(fan, postId).andExpect(status().isNoContent()); // repetir não conta de novo

        eventually(() -> engagementOf(postId, fan)
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true)));

        like(other, postId).andExpect(status().isNoContent());

        eventually(() -> engagementOf(postId, fan)
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true)));

        eventually(() -> engagementOf(postId, fan)
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true)));
    }

    @Test
    void unlikeDecrementsAndNeverGoesBelowZero() throws Exception {
        User author = newUser();
        User fan = newUser();
        String postId = createPost(author, "vai e volta");

        like(fan, postId).andExpect(status().isNoContent());
        eventually(() -> engagementOf(postId, fan)
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true)));

        unlike(fan, postId).andExpect(status().isNoContent());
        eventually(() -> engagementOf(postId, fan)
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.likedByMe").value(false)));

        unlike(fan, postId).andExpect(status().isNoContent());
        Thread.sleep(1200);
        engagementOf(postId, fan).andExpect(jsonPath("$.likeCount").value(0));
    }

    @Test
    void likingRequiresAuthentication() throws Exception {
        User author = newUser();
        String postId = createPost(author, "só logado curte");

        mvc.perform(put("/api/v1/posts/" + postId + "/like")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/posts/" + postId + "/like")).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownOrDeletedPostsCannotBeLiked() throws Exception {
        User author = newUser();
        User fan = newUser();

        like(fan, "000000000000000000000000").andExpect(status().isNotFound());
        like(fan, "abc").andExpect(status().isNotFound());
        engagementOf("000000000000000000000000", null).andExpect(status().isNotFound());

        String postId = createPost(author, "efêmero");
        mvc.perform(delete("/api/v1/posts/" + postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + author.token()))
                .andExpect(status().isNoContent());

        like(fan, postId).andExpect(status().isNotFound());
    }

    @Test
    void concurrentLikesAreCountedOncePerUser() throws Exception {
        User author = newUser();
        String postId = createPost(author, "disputado");
        List<User> fans = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            fans.add(newUser());
        }

        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (User fan : fans) {
                for (int i = 0; i < 2; i++) {
                    tasks.add(() -> mvc.perform(put("/api/v1/posts/" + postId + "/like")
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + fan.token()))
                            .andReturn().getResponse().getStatus());
                }
            }
            for (Future<Integer> result : pool.invokeAll(tasks)) {
                assertEquals(204, result.get());
            }
        } finally {
            pool.shutdown();
        }

        eventually(() -> engagementOf(postId, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(8)));
    }
    @Autowired
    ApplicationEventPublisher publisher;
    @Test
    void duplicateDeliveriesOfTheSameEventCountOnce() throws Exception {
        User author = newUser();
        String postId = createPost(author, "entregue duas vezes");
        String likeId = "manual-" + UUID.randomUUID();

        publisher.publishEvent(new PostLiked(likeId, postId, "alguem"));
        publisher.publishEvent(new PostLiked(likeId, postId, "alguem"));

        eventually(() -> engagementOf(postId, null).andExpect(jsonPath("$.likeCount").value(1)));
    }

    // --- helpers ---

    private ResultActions like(User user, String postId) throws Exception {
        return mvc.perform(put("/api/v1/posts/" + postId + "/like")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token()));
    }

    private ResultActions unlike(User user, String postId) throws Exception {
        return mvc.perform(delete("/api/v1/posts/" + postId + "/like")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token()));
    }

    private ResultActions engagementOf(String postId, User viewer) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/posts/" + postId + "/engagement");
        if (viewer != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + viewer.token());
        }
        return mvc.perform(request);
    }

    private User newUser() throws Exception {
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

    private String createPost(User user, String text) throws Exception {
        String body = mvc.perform(post("/api/v1/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"%s"}
                                """.formatted(text)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
    private static void eventually(ThrowingRunnable assertion) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(assertion);
    }
}