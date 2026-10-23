package com.leviberga.bafafa.timeline.application;

public final class TimelineErrors {

    private TimelineErrors() {
    }

    public static class AuthorNotFound extends RuntimeException {
        public AuthorNotFound() {
            super("Perfil não encontrado");
        }
    }
}