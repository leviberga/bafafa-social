package com.leviberga.bafafa.timeline.application;

import java.time.Instant;
import java.util.List;

public final class Timeline {

    private Timeline() {
    }

    public record Page(List<Item> items, String nextCursor) {
    }

    public record Item(String kind, PostCard post, Actor repostedBy) {
    }

    public record PostCard(String id, Actor author, String text, Instant createdAt, Engagement engagement) {
    }

    public record Actor(String id, String handle, String displayName) {
    }

    public record Engagement(long likeCount, long repostCount, boolean likedByMe, boolean repostedByMe) {
    }
}