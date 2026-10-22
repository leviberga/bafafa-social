package com.leviberga.bafafa.engagement.domain;

public interface PostStatsOperations {

    void adjustLikes(String postId, int delta);

    void adjustReposts(String postId, int delta);
}