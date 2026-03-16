package com.khaleo.flashcard.service.auth;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthDomainException extends RuntimeException {

    private final HttpStatus status;
    private final AuthErrorCode errorCode;

    public AuthDomainException(HttpStatus status, AuthErrorCode errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
