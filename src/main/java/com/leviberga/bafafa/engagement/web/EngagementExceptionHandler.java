package com.leviberga.bafafa.engagement.web;

import com.leviberga.bafafa.engagement.application.EngagementErrors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class EngagementExceptionHandler {

    @ExceptionHandler(EngagementErrors.PostNotFound.class)
    ProblemDetail postNotFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Post não encontrado.");
    }
}