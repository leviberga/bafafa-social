package com.leviberga.bafafa.graph.web;

import com.leviberga.bafafa.graph.application.FollowService;
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
@RequestMapping("/api/v1/profiles/{handle}")
class GraphController {

    private final FollowService follows;

    GraphController(FollowService follows) {
        this.follows = follows;
    }

    @PutMapping("/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void follow(@AuthenticationPrincipal Jwt jwt, @PathVariable String handle) {
        follows.follow(UUID.fromString(jwt.getSubject()), handle);
    }

    @DeleteMapping("/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unfollow(@AuthenticationPrincipal Jwt jwt, @PathVariable String handle) {
        follows.unfollow(UUID.fromString(jwt.getSubject()), handle);
    }

    /** Público. Se vier um token válido, informa também se o usuário já segue o perfil. */
    @GetMapping("/follow-stats")
    FollowService.FollowStats stats(@AuthenticationPrincipal Jwt jwt, @PathVariable String handle) {
        UUID viewerId = jwt == null ? null : UUID.fromString(jwt.getSubject());
        return follows.stats(handle, viewerId);
    }
}