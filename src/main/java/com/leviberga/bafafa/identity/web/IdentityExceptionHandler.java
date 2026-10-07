package com.leviberga.bafafa.identity.web;

import com.leviberga.bafafa.identity.application.AccountConflictException;
import com.leviberga.bafafa.identity.application.InvalidCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class IdentityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail invalidCredentials() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
    }

    @ExceptionHandler(AccountConflictException.class)
    ProblemDetail conflict(AccountConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}