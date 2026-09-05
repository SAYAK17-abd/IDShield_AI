package com.project.exception;

import org.springframework.http.HttpStatus;

public class InvalidFileException extends ApiException {
    public InvalidFileException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_FILE");
    }
}

