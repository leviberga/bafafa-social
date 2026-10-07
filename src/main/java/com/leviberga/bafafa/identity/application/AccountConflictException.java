package com.leviberga.bafafa.identity.application;

public class AccountConflictException extends RuntimeException {
    public AccountConflictException(String field) {
        super("Já existe uma conta com este " + field + ".");
    }
}