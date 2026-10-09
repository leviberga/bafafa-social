package com.leviberga.bafafa.identity.application;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Refresh token inválido");
    }
}