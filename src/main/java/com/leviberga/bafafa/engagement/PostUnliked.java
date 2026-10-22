package com.leviberga.bafafa.engagement;

import com.leviberga.bafafa.outbox.OutboxEvent;

public record PostUnliked(String likeId, String postId, String userId) implements OutboxEvent {
}