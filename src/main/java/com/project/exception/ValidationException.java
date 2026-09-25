package com.project.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when input data validation fails (e.g. invalid OTP, failed captcha).
 */
public class ValidationException extends ApiException {
    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
    }

    public ValidationException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }
}
