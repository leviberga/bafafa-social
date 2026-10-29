package com.leviberga.bafafa.timeline.web;

import com.leviberga.bafafa.timeline.application.Timeline;
import com.leviberga.bafafa.timeline.application.TimelineService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/timeline")
class TimelineController {

    private final TimelineService timeline;

    TimelineController(TimelineService timeline) {
        this.timeline = timeline;
    }

    @GetMapping("/home")
    Timeline.Page home(@AuthenticationPrincipal Jwt jwt,
                       @RequestParam(required = false) String cursor,
                       @RequestParam(defaultValue = "20") int limit) {
        return timeline.home(UUID.fromString(jwt.getSubject()), cursor, limit);
    }

    @GetMapping("/users/{handle}")
    Timeline.Page ofUser(@AuthenticationPrincipal Jwt jwt, @PathVariable String handle,
                         @RequestParam(required = false) String cursor,
                         @RequestParam(defaultValue = "20") int limit) {
        UUID viewerId = jwt == null ? null : UUID.fromString(jwt.getSubject());
        return timeline.ofUser(handle, viewerId, cursor, limit);
    }

    @GetMapping("/posts/{id}")
    Timeline.PostThread post(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return timeline.thread(id, viewerId(jwt));
    }

    @GetMapping("/posts/{id}/replies")
    Timeline.Page replies(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
                          @RequestParam(required = false) String cursor,
                          @RequestParam(defaultValue = "20") int limit) {
        return timeline.replies(id, viewerId(jwt), cursor, limit);
    }

    private static UUID viewerId(Jwt jwt) {
        return jwt == null ? null : UUID.fromString(jwt.getSubject());
    }
}