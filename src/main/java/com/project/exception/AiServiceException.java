package com.project.exception;

import org.springframework.http.HttpStatus;

public class AiServiceException extends ApiException {
    public AiServiceException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_ERROR");
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_ERROR");
        initCause(cause);
    }
}

