package com.leviberga.bafafa.post;

import com.jayway.jsonpath.JsonPath;
import com.leviberga.bafafa.TestApi;
import com.leviberga.bafafa.TestApi.User;
import com.leviberga.bafafa.TestcontainersConfiguration;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.leviberga.bafafa.TestApi.createPost;
import static com.leviberga.bafafa.TestApi.newUser;
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
class RepostFlowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void repostIsIdempotentAppearsInTheAuthorListingAndIsCounted() throws Exception {
        User author = newUser(mvc);
        User reposter = newUser(mvc);
        String originalId = createPost(mvc, author, "original");

        String firstBody = repost(reposter, originalId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("REPOST"))
                .andExpect(jsonPath("$.repostOfId").value(originalId))
                .andReturn().getResponse().getContentAsString();
        String secondBody = repost(reposter, originalId)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String firstId = JsonPath.read(firstBody, "$.id");
        String secondId = JsonPath.read(secondBody, "$.id");
        assertEquals(firstId, secondId); // idempotente: é o mesmo repost

        mvc.perform(get("/api/v1/posts").param("author", reposter.handle()))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].type").value("REPOST"))
                .andExpect(jsonPath("$.items[0].repostOfId").value(originalId));

        eventually(() -> engagementOf(originalId, reposter)
                .andExpect(jsonPath("$.repostCount").value(1))
                .andExpect(jsonPath("$.repostedByMe").value(true)));
        engagementOf(originalId, author)
                .andExpect(jsonPath("$.repostedByMe").value(false));
    }

    @Test
    void unrepostRemovesItAndReleasesTheSlot() throws Exception {
        User author = newUser(mvc);
        User reposter = newUser(mvc);
        String originalId = createPost(mvc, author, "vai e volta");

        repost(reposter, originalId).andExpect(status().isOk());
        eventually(() -> engagementOf(originalId, reposter)
                .andExpect(jsonPath("$.repostCount").value(1)));

        unrepost(reposter, originalId).andExpect(status().isNoContent());
        unrepost(reposter, originalId).andExpect(status().isNoContent()); // idempotente

        mvc.perform(get("/api/v1/posts").param("author", reposter.handle()))
                .andExpect(jsonPath("$.items.length()").value(0));
        eventually(() -> engagementOf(originalId, reposter)
                .andExpect(jsonPath("$.repostCount").value(0))
                .andExpect(jsonPath("$.repostedByMe").value(false)));

        repost(reposter, originalId).andExpect(status().isOk());
        eventually(() -> engagementOf(originalId, reposter)
                .andExpect(jsonPath("$.repostCount").value(1)));
    }

    @Test
    void repostsCannotBeRepostedRepliedToOrLiked() throws Exception {
        User author = newUser(mvc);
        User reposter = newUser(mvc);
        String originalId = createPost(mvc, author, "original");

        String repostBody = repost(reposter, originalId).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String repostId = JsonPath.read(repostBody, "$.id");

        repost(author, repostId).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/posts/" + repostId + "/replies")
                        .header(HttpHeaders.AUTHORIZATION, author.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"resposta"}
                                """))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/posts/" + repostId + "/like")
                        .header(HttpHeaders.AUTHORIZATION, author.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownPostsAndMissingAuthenticationAreRejected() throws Exception {
        User user = newUser(mvc);

        repost(user, "000000000000000000000000").andExpect(status().isNotFound());
        repost(user, "abc").andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/posts/000000000000000000000000/repost")).andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentRepostsFromTheSameUserCreateASingleRepost() throws Exception {
        User author = newUser(mvc);
        User reposter = newUser(mvc);
        String originalId = createPost(mvc, author, "disputado");

        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                tasks.add(() -> repost(reposter, originalId).andReturn().getResponse().getStatus());
            }
            for (Future<Integer> result : pool.invokeAll(tasks)) {
                assertEquals(200, result.get());
            }
        } finally {
            pool.shutdown();
        }

        mvc.perform(get("/api/v1/posts").param("author", reposter.handle()))
                .andExpect(jsonPath("$.items.length()").value(1));

        eventually(() -> engagementOf(originalId, null)
                .andExpect(jsonPath("$.repostCount").value(1)));
    }

    // --- helpers ---

    private ResultActions repost(User user, String postId) throws Exception {
        return mvc.perform(put("/api/v1/posts/" + postId + "/repost")
                .header(HttpHeaders.AUTHORIZATION, user.bearer()));
    }

    private ResultActions unrepost(User user, String postId) throws Exception {
        return mvc.perform(delete("/api/v1/posts/" + postId + "/repost")
                .header(HttpHeaders.AUTHORIZATION, user.bearer()));
    }

    private ResultActions engagementOf(String postId, User viewer) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/posts/" + postId + "/engagement");
        if (viewer != null) {
            request.header(HttpHeaders.AUTHORIZATION, viewer.bearer());
        }
        return mvc.perform(request);
    }
    private static void eventually(ThrowingRunnable assertion) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(assertion);
    }
}