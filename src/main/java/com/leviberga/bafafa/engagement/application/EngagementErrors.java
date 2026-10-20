package com.leviberga.bafafa.engagement.application;

public final class EngagementErrors {

    private EngagementErrors() {
    }

    public static class PostNotFound extends RuntimeException {
        public PostNotFound() {
            super("Post não encontrado");
        }
    }
}