package com.leviberga.bafafa.identity.application;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Credenciais inválidas");
    }
}