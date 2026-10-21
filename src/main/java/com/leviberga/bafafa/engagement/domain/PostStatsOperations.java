package com.leviberga.bafafa.engagement.domain;

public interface PostStatsOperations {

    void incrementLikes(String postId);

    void decrementLikes(String postId);

    void incrementReposts(String postId);

    void decrementReposts(String postId);
}