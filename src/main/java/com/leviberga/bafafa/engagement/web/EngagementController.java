package com.leviberga.bafafa.engagement.web;

import com.leviberga.bafafa.engagement.application.LikeService;
import com.leviberga.bafafa.engagement.web.EngagementDtos.EngagementResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts/{postId}")
class EngagementController {

    private final LikeService likes;

    EngagementController(LikeService likes) {
        this.likes = likes;
    }

    @PutMapping("/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void like(@AuthenticationPrincipal Jwt jwt, @PathVariable String postId) {
        likes.like(UUID.fromString(jwt.getSubject()), postId);
    }

    @DeleteMapping("/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unlike(@AuthenticationPrincipal Jwt jwt, @PathVariable String postId) {
        likes.unlike(UUID.fromString(jwt.getSubject()), postId);
    }

    /** Público. Se vier um token válido, informa também se o usuário curtiu. */
    @GetMapping("/engagement")
    EngagementResponse engagement(@AuthenticationPrincipal Jwt jwt, @PathVariable String postId) {
        UUID viewerId = jwt == null ? null : UUID.fromString(jwt.getSubject());
        return EngagementResponse.from(likes.get(postId, viewerId));
    }
}