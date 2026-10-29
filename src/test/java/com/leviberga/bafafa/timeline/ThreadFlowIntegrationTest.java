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

import static com.leviberga.bafafa.TestApi.createPost;
import static com.leviberga.bafafa.TestApi.newUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ThreadFlowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void aPostThreadIsHydratedAndPublic() throws Exception {
        User author = newUser(mvc);
        String postId = createPost(mvc, author, "post raiz");

        mvc.perform(get("/api/v1/timeline/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.post.id").value(postId))
                .andExpect(jsonPath("$.post.text").value("post raiz"))
                .andExpect(jsonPath("$.post.author.handle").value(author.handle()))
                .andExpect(jsonPath("$.post.engagement.likedByMe").value(false))
                .andExpect(jsonPath("$.inReplyTo").doesNotExist());
    }

    @Test
    void aReplyKnowsWhoItRepliesToEvenAfterTheOriginalIsDeleted() throws Exception {
        User author = newUser(mvc);
        User fan = newUser(mvc);
        String rootId = createPost(mvc, author, "post raiz");
        String replyId = reply(fan, rootId, "minha resposta");

        mvc.perform(get("/api/v1/timeline/posts/" + replyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.post.author.handle").value(fan.handle()))
                .andExpect(jsonPath("$.inReplyTo.postId").value(rootId))
                .andExpect(jsonPath("$.inReplyTo.author.handle").value(author.handle()));

        mvc.perform(delete("/api/v1/posts/" + rootId).header(HttpHeaders.AUTHORIZATION, author.bearer()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/timeline/posts/" + replyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inReplyTo.postId").value(rootId))
                .andExpect(jsonPath("$.inReplyTo.author").doesNotExist());
    }

    @Test
    void repliesAreListedOldestFirstWithAuthorsAndPaginated() throws Exception {
        User author = newUser(mvc);
        User fan = newUser(mvc);
        String rootId = createPost(mvc, author, "post raiz");
        String r1 = reply(fan, rootId, "r1");
        reply(fan, rootId, "r2");
        reply(author, rootId, "r3");
        reply(author, r1, "resposta da resposta"); // não é resposta direta do post raiz

        String firstPage = mvc.perform(get("/api/v1/timeline/posts/" + rootId + "/replies").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].kind").value("REPLY"))
                .andExpect(jsonPath("$.items[0].post.text").value("r1"))
                .andExpect(jsonPath("$.items[0].post.author.handle").value(fan.handle()))
                .andExpect(jsonPath("$.items[1].post.text").value("r2"))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String cursor = JsonPath.read(firstPage, "$.nextCursor");

        mvc.perform(get("/api/v1/timeline/posts/" + rootId + "/replies")
                        .param("limit", "2").param("cursor", cursor))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].post.text").value("r3"))
                .andExpect(jsonPath("$.items[0].post.author.handle").value(author.handle()))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void viewersSeeTheirOwnStateOnTheThread() throws Exception {
        User author = newUser(mvc);
        User fan = newUser(mvc);
        String postId = createPost(mvc, author, "curtido");

        mvc.perform(put("/api/v1/posts/" + postId + "/like").header(HttpHeaders.AUTHORIZATION, fan.bearer()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/timeline/posts/" + postId).header(HttpHeaders.AUTHORIZATION, fan.bearer()))
                .andExpect(jsonPath("$.post.engagement.likedByMe").value(true));
        mvc.perform(get("/api/v1/timeline/posts/" + postId))
                .andExpect(jsonPath("$.post.engagement.likedByMe").value(false));
    }

    @Test
    void unknownInvalidAndRepostIdsAreNotFound() throws Exception {
        User author = newUser(mvc);
        User reposter = newUser(mvc);
        String postId = createPost(mvc, author, "original");

        String repostBody = mvc.perform(put("/api/v1/posts/" + postId + "/repost")
                        .header(HttpHeaders.AUTHORIZATION, reposter.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String repostId = JsonPath.read(repostBody, "$.id");

        mvc.perform(get("/api/v1/timeline/posts/000000000000000000000000")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/timeline/posts/abc")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/timeline/posts/" + repostId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/timeline/posts/" + repostId + "/replies")).andExpect(status().isNotFound());
    }

    private String reply(User user, String parentId, String text) throws Exception {
        String body = mvc.perform(post("/api/v1/posts/" + parentId + "/replies")
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"%s"}
                                """.formatted(text)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}