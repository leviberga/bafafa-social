package com.leviberga.bafafa.timeline.application;

public final class TimelineErrors {

    private TimelineErrors() {
    }

    public static class AuthorNotFound extends RuntimeException {
        public AuthorNotFound() {
            super("Perfil não encontrado");
        }
    }

    public static class PostNotFound extends RuntimeException {
        public PostNotFound() {
            super("Post não encontrado");
        }
    }
}