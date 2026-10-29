package com.leviberga.bafafa.timeline;

import com.jayway.jsonpath.JsonPath;
import com.leviberga.bafafa.TestApi.User;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.awaitility.core.ThrowingRunnable;

import java.time.Duration;

import static com.leviberga.bafafa.TestApi.createPost;
import static com.leviberga.bafafa.TestApi.newUser;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TimelineFlowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void homeShowsOwnAndFollowedPostsNewestFirstWithoutRepliesOrStrangers() throws Exception {
        User author = newUser(mvc);
        User fan = newUser(mvc);
        User stranger = newUser(mvc);
        follow(fan, author.handle());

        String p1 = createPost(mvc, author, "p1");
        createPost(mvc, author, "p2");
        createPost(mvc, stranger, "q1");
        createPost(mvc, fan, "f1");
        reply(author, p1, "r1");

        home(fan, null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].kind").value("POST"))
                .andExpect(jsonPath("$.items[0].post.text").value("f1"))
                .andExpect(jsonPath("$.items[0].post.author.handle").value(fan.handle()))
                .andExpect(jsonPath("$.items[1].post.text").value("p2"))
                .andExpect(jsonPath("$.items[1].post.author.handle").value(author.handle()))
                .andExpect(jsonPath("$.items[2].post.text").value("p1"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void homeIsPaginatedByCursor() throws Exception {
        User author = newUser(mvc);
        User fan = newUser(mvc);
        follow(fan, author.handle());
        createPost(mvc, author, "a");
        createPost(mvc, author, "b");
        createPost(mvc, author, "c");

        String firstPage = home(fan, null, 2)
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].post.text").value("c"))
                .andExpect(jsonPath("$.items[1].post.text").value("b"))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String cursor = JsonPath.read(firstPage, "$.nextCursor");

        home(fan, cursor, 2)
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].post.text").value("a"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void itemsCarryEngagementForTheViewer() throws Exception {
        User author = newUser(mvc);
        User fan = newUser(mvc);
        follow(fan, author.handle());
        String postId = createPost(mvc, author, "curtido");

        mvc.perform(put("/api/v1/posts/" + postId + "/like").header(HttpHeaders.AUTHORIZATION, fan.bearer()))
                .andExpect(status().isNoContent());

        // O número chega pelo outbox (assíncrono); "likedByMe" vem direto do banco.
        eventually(() -> home(fan, null, null)
                .andExpect(jsonPath("$.items[0].post.engagement.likeCount").value(1))
                .andExpect(jsonPath("$.items[0].post.engagement.likedByMe").value(true))
                .andExpect(jsonPath("$.items[0].post.engagement.repostedByMe").value(false)));
    }

    @Test
    void repostsShowTheOriginalAndDisappearWhenTheOriginalIsDeleted() throws Exception {
        User stranger = newUser(mvc);
        User fan = newUser(mvc);
        String originalId = createPost(mvc, stranger, "post original");

        mvc.perform(put("/api/v1/posts/" + originalId + "/repost").header(HttpHeaders.AUTHORIZATION, fan.bearer()))
                .andExpect(status().isOk());

        home(fan, null, null)
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].kind").value("REPOST"))
                .andExpect(jsonPath("$.items[0].post.text").value("post original"))
                .andExpect(jsonPath("$.items[0].post.author.handle").value(stranger.handle()))
                .andExpect(jsonPath("$.items[0].repostedBy.handle").value(fan.handle()))
                .andExpect(jsonPath("$.items[0].post.engagement.repostedByMe").value(true));

        mvc.perform(delete("/api/v1/posts/" + originalId).header(HttpHeaders.AUTHORIZATION, stranger.bearer()))
                .andExpect(status().isNoContent());

        home(fan, null, null).andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void unfollowingRemovesTheirPostsFromTheFeed() throws Exception {
        User author = newUser(mvc);
        User fan = newUser(mvc);
        follow(fan, author.handle());
        createPost(mvc, author, "visível");

        home(fan, null, null).andExpect(jsonPath("$.items.length()").value(1));

        mvc.perform(delete("/api/v1/profiles/" + author.handle() + "/follow")
                        .header(HttpHeaders.AUTHORIZATION, fan.bearer()))
                .andExpect(status().isNoContent());

        home(fan, null, null).andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void userTimelineIsPublicAndHidesReplies() throws Exception {
        User author = newUser(mvc);
        String postId = createPost(mvc, author, "a1");
        reply(author, postId, "resposta");

        mvc.perform(get("/api/v1/timeline/users/" + author.handle()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].post.text").value("a1"))
                .andExpect(jsonPath("$.items[0].post.engagement.likedByMe").value(false));

        mvc.perform(get("/api/v1/timeline/users/ninguem_assim")).andExpect(status().isNotFound());
    }

    @Test
    void homeRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/timeline/home")).andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private ResultActions home(User viewer, String cursor, Integer limit) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/timeline/home")
                .header(HttpHeaders.AUTHORIZATION, viewer.bearer());
        if (cursor != null) {
            request.param("cursor", cursor);
        }
        if (limit != null) {
            request.param("limit", String.valueOf(limit));
        }
        return mvc.perform(request);
    }

    private void follow(User follower, String handle) throws Exception {
        mvc.perform(put("/api/v1/profiles/" + handle + "/follow")
                        .header(HttpHeaders.AUTHORIZATION, follower.bearer()))
                .andExpect(status().isNoContent());
    }

    private void reply(User user, String parentId, String text) throws Exception {
        mvc.perform(post("/api/v1/posts/" + parentId + "/replies")
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"%s"}
                                """.formatted(text)))
                .andExpect(status().isCreated());
    }

    private static void eventually(ThrowingRunnable assertion) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(assertion);
    }
}