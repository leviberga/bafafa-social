package com.leviberga.bafafa.post;

import com.leviberga.bafafa.outbox.OutboxEvent;

public record RepostRemoved(String originalPostId, String repostId, String userId) implements OutboxEvent {
}