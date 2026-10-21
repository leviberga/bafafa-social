package com.leviberga.bafafa.post.web;

import com.leviberga.bafafa.post.application.PostErrors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class PostExceptionHandler {

    @ExceptionHandler(PostErrors.PostNotFound.class)
    ProblemDetail postNotFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Post não encontrado.");
    }

    @ExceptionHandler(PostErrors.AuthorNotFound.class)
    ProblemDetail authorNotFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Autor não encontrado.");
    }

    @ExceptionHandler(PostErrors.NotOwner.class)
    ProblemDetail notOwner() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Você só pode apagar os seus próprios posts.");
    }

    @ExceptionHandler(PostErrors.InvalidCursor.class)
    ProblemDetail invalidCursor() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Cursor inválido.");
    }

    @ExceptionHandler(PostErrors.NotInteractable.class)
    ProblemDetail notInteractable() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Reposts não podem receber respostas nem ser repostados.");
    }
}