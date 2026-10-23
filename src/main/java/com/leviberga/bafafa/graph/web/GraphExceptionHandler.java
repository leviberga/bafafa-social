package com.leviberga.bafafa.graph.web;

import com.leviberga.bafafa.graph.application.GraphErrors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GraphExceptionHandler {

    @ExceptionHandler(GraphErrors.ProfileNotFound.class)
    ProblemDetail profileNotFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Perfil não encontrado.");
    }

    @ExceptionHandler(GraphErrors.CannotFollowSelf.class)
    ProblemDetail cannotFollowSelf() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Não é possível seguir a si mesmo.");
    }

    @ExceptionHandler(GraphErrors.FollowLimitReached.class)
    ProblemDetail limitReached() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Limite de contas seguidas atingido.");
    }
}