package com.leviberga.bafafa.post;

import java.time.Instant;

public record PostView(String id, String authorId, String type, String parentId, String rootId,
                       String text, Instant createdAt, String repostOfId) {
}