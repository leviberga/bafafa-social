package com.leviberga.bafafa.post;

import com.jayway.jsonpath.JsonPath;
import com.leviberga.bafafa.TestClient;
import com.leviberga.bafafa.TestcontainersConfiguration;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.mongodb.assertions.Assertions.assertNotNull;
import static com.mongodb.assertions.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PostFlowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    StringRedisTemplate redis;

    @Autowired
    MongoTemplate mongo;

    private record User(String handle, String token) {
    }

    @Test
    void createdPostCanBeReadPublicly() throws Exception {
        User author = newUser();
        String postId = createPost(author, "Primeiro bafafá!");

        mvc.perform(get("/api/v1/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Primeiro bafafá!"))
                .andExpect(jsonPath("$.type").value("POST"))
                .andExpect(jsonPath("$.parentId").doesNotExist());
    }

    @Test
    void creatingPostRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/posts")
                        .with(TestClient.newIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"sem login"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTextIsRejected() throws Exception {
        User author = newUser();

        mvc.perform(post("/api/v1/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + author.token())
                        .with(TestClient.newIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"   "}
                                """))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + author.token())
                        .with(TestClient.newIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + "a".repeat(281) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void repliesFormAThreadWithTheSameRoot() throws Exception {
        User author = newUser();
        User other = newUser();
        String rootId = createPost(author, "post raiz");
        String firstReplyId = reply(other, rootId, "primeira resposta");

        mvc.perform(post("/api/v1/posts/" + firstReplyId + "/replies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + author.token())
                        .with(TestClient.newIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"resposta da resposta"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("REPLY"))
                .andExpect(jsonPath("$.parentId").value(firstReplyId))
                .andExpect(jsonPath("$.rootId").value(rootId));

        mvc.perform(get("/api/v1/posts/" + rootId + "/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(firstReplyId))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void authorTimelineIsPaginatedByCursorNewestFirst() throws Exception {
        User author = newUser();
        createPost(author, "um");
        createPost(author, "dois");
        createPost(author, "tres");

        String firstPage = mvc.perform(get("/api/v1/posts").param("author", author.handle()).param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].text").value("tres"))
                .andExpect(jsonPath("$.items[1].text").value("dois"))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String cursor = JsonPath.read(firstPage, "$.nextCursor");

        mvc.perform(get("/api/v1/posts").param("author", author.handle()).param("limit", "2").param("cursor", cursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].text").value("um"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void onlyTheOwnerCanDeleteAndDeletedPostsDisappear() throws Exception {
        User owner = newUser();
        User other = newUser();
        String postId = createPost(owner, "vou apagar");

        mvc.perform(delete("/api/v1/posts/" + postId).header(HttpHeaders.AUTHORIZATION, "Bearer " + other.token()))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/v1/posts/" + postId).header(HttpHeaders.AUTHORIZATION, "Bearer " + owner.token()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/posts/" + postId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/posts").param("author", owner.handle()))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void unknownOrMalformedIdsAreNotFound() throws Exception {
        mvc.perform(get("/api/v1/posts/000000000000000000000000")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/posts/abc")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/posts").param("author", "ninguem_assim")).andExpect(status().isNotFound());
    }

    @Test
    void malformedCursorIsBadRequest() throws Exception {
        User author = newUser();
        mvc.perform(get("/api/v1/posts").param("author", author.handle()).param("cursor", "lixo"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void rateLimitIsPerUserAndPerAction() throws Exception {
        User spammer = newUser();
        User other = newUser();
        String othersPostId = createPost(other, "post de outra pessoa");

        for (int i = 0; i < 10; i++) {
            createPost(spammer, "post " + i);
        }

        mvc.perform(post("/api/v1/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + spammer.token())
                        .with(TestClient.newIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"text":"um a mais"}
                            """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));

        reply(spammer, othersPostId, "mas ainda posso responder");
        createPost(other, "eu ainda posso postar");
    }
    @Test
    void readsAreServedFromRedisAfterTheFirstHit() throws Exception {
        User author = newUser();
        String postId = createPost(author, "post popular");
        String cacheKey = "post:v1:" + postId;

        assertNull(redis.opsForValue().get(cacheKey));

        mvc.perform(get("/api/v1/posts/" + postId)).andExpect(status().isOk());
        assertNotNull(redis.opsForValue().get(cacheKey));

        mongo.remove(Query.query(Criteria.where("_id").is(new ObjectId(postId))), "posts");
        mvc.perform(get("/api/v1/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("post popular"));

        redis.delete(cacheKey);
        mvc.perform(get("/api/v1/posts/" + postId)).andExpect(status().isNotFound());
    }

    @Test
    void deletingAPostEvictsItFromTheCache() throws Exception {
        User author = newUser();
        String postId = createPost(author, "efêmero");
        String cacheKey = "post:v1:" + postId;

        mvc.perform(get("/api/v1/posts/" + postId)).andExpect(status().isOk());
        assertNotNull(redis.opsForValue().get(cacheKey));

        mvc.perform(delete("/api/v1/posts/" + postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + author.token()))
                .andExpect(status().isNoContent());

        assertNull(redis.opsForValue().get(cacheKey));
        mvc.perform(get("/api/v1/posts/" + postId)).andExpect(status().isNotFound());
    }

    // --- helpers ---

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
                        .with(TestClient.newIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"%s"}
                                """.formatted(text)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String reply(User user, String parentId, String text) throws Exception {
        String body = mvc.perform(post("/api/v1/posts/" + parentId + "/replies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token())
                        .with(TestClient.newIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"%s"}
                                """.formatted(text)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}