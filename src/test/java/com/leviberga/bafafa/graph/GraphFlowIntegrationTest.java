package com.leviberga.bafafa.graph;

import com.leviberga.bafafa.TestApi.User;
import com.leviberga.bafafa.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.leviberga.bafafa.TestApi.newUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GraphFlowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void followIsIdempotentAndCounted() throws Exception {
        User ana = newUser(mvc);
        User bia = newUser(mvc);

        follow(ana, bia.handle()).andExpect(status().isNoContent());
        follow(ana, bia.handle()).andExpect(status().isNoContent());

        stats(bia.handle(), ana)
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followingCount").value(0))
                .andExpect(jsonPath("$.followedByMe").value(true));
        stats(ana.handle(), ana).andExpect(jsonPath("$.followingCount").value(1));
        stats(bia.handle(), null).andExpect(jsonPath("$.followedByMe").value(false));

        unfollow(ana, bia.handle()).andExpect(status().isNoContent());
        unfollow(ana, bia.handle()).andExpect(status().isNoContent());

        stats(bia.handle(), ana)
                .andExpect(jsonPath("$.followerCount").value(0))
                .andExpect(jsonPath("$.followedByMe").value(false));
    }

    @Test
    void invalidFollowsAreRejected() throws Exception {
        User ana = newUser(mvc);

        follow(ana, ana.handle()).andExpect(status().isBadRequest());
        follow(ana, "ninguem_assim").andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/profiles/" + ana.handle() + "/follow")).andExpect(status().isUnauthorized());
    }

    private ResultActions follow(User user, String handle) throws Exception {
        return mvc.perform(put("/api/v1/profiles/" + handle + "/follow")
                .header(HttpHeaders.AUTHORIZATION, user.bearer()));
    }

    private ResultActions unfollow(User user, String handle) throws Exception {
        return mvc.perform(delete("/api/v1/profiles/" + handle + "/follow")
                .header(HttpHeaders.AUTHORIZATION, user.bearer()));
    }

    private ResultActions stats(String handle, User viewer) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/profiles/" + handle + "/follow-stats");
        if (viewer != null) {
            request.header(HttpHeaders.AUTHORIZATION, viewer.bearer());
        }
        return mvc.perform(request);
    }
}