package com.leviberga.bafafa.post.application;

public final class PostErrors {

    private PostErrors() {
    }

    public static class PostNotFound extends RuntimeException {
        public PostNotFound() {
            super("Post não encontrado");
        }
    }

    public static class AuthorNotFound extends RuntimeException {
        public AuthorNotFound() {
            super("Autor não encontrado");
        }
    }

    public static class NotOwner extends RuntimeException {
        public NotOwner() {
            super("Apenas o autor pode apagar o post");
        }
    }

    public static class InvalidCursor extends RuntimeException {
        public InvalidCursor() {
            super("Cursor inválido");
        }
    }
    public static class NotInteractable extends RuntimeException {
        public NotInteractable() {
            super("Este post não aceita respostas nem reposts");
        }
    }
}