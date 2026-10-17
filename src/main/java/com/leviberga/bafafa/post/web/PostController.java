package com.leviberga.bafafa.post.web;

import com.leviberga.bafafa.post.application.PostService;
import com.leviberga.bafafa.post.web.PostDtos.CreatePostRequest;
import com.leviberga.bafafa.post.web.PostDtos.PostPageResponse;
import com.leviberga.bafafa.post.web.PostDtos.PostResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
class PostController {

    private final PostService posts;

    PostController(PostService posts) {
        this.posts = posts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PostResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreatePostRequest request) {
        return PostResponse.from(posts.create(accountId(jwt), request.text()));
    }

    @GetMapping("/{id}")
    PostResponse get(@PathVariable String id) {
        return PostResponse.from(posts.get(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        posts.delete(accountId(jwt), id);
    }

    @PostMapping("/{id}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    PostResponse reply(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
                       @Valid @RequestBody CreatePostRequest request) {
        return PostResponse.from(posts.reply(accountId(jwt), id, request.text()));
    }

    @GetMapping("/{id}/replies")
    PostPageResponse replies(@PathVariable String id,
                             @RequestParam(required = false) String cursor,
                             @RequestParam(defaultValue = "20") int limit) {
        return PostPageResponse.from(posts.listReplies(id, cursor, limit));
    }

    @GetMapping
    PostPageResponse byAuthor(@RequestParam String author,
                              @RequestParam(required = false) String cursor,
                              @RequestParam(defaultValue = "20") int limit) {
        return PostPageResponse.from(posts.listByAuthor(author, cursor, limit));
    }

    private static UUID accountId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}