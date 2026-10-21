package com.leviberga.bafafa.post.web;

import com.leviberga.bafafa.post.application.PostService;
import com.leviberga.bafafa.post.web.PostDtos.CreatePostRequest;
import com.leviberga.bafafa.post.web.PostDtos.PostPageResponse;
import com.leviberga.bafafa.post.web.PostDtos.PostResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/{id}/repost")
    PostResponse repost(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return PostResponse.from(posts.repost(accountId(jwt), id));
    }

    @DeleteMapping("/{id}/repost")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unrepost(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        posts.unrepost(accountId(jwt), id);
    }

    private static UUID accountId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}