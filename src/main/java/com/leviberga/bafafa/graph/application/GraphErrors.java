package com.leviberga.bafafa.graph.application;

public final class GraphErrors {

    private GraphErrors() {
    }

    public static class ProfileNotFound extends RuntimeException {
        public ProfileNotFound() {
            super("Perfil não encontrado");
        }
    }

    public static class CannotFollowSelf extends RuntimeException {
        public CannotFollowSelf() {
            super("Não é possível seguir a si mesmo");
        }
    }

    public static class FollowLimitReached extends RuntimeException {
        public FollowLimitReached() {
            super("Limite de contas seguidas atingido");
        }
    }
}