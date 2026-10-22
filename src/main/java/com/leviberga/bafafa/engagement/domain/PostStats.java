package com.leviberga.bafafa.engagement.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "post_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostStats {

    @Id
    private String postId;

    private long likeCount;

    private long repostCount;

    public long likeCountOrZero() {
        return Math.max(0, likeCount);
    }

    public long repostCountOrZero() {
        return Math.max(0, repostCount);
    }
}