package com.leviberga.bafafa.timeline.web;

import com.leviberga.bafafa.timeline.application.TimelineErrors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class TimelineExceptionHandler {

    @ExceptionHandler(TimelineErrors.AuthorNotFound.class)
    ProblemDetail authorNotFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Perfil não encontrado.");
    }
}