package com.khaleo.flashcard.service.study;

import com.khaleo.flashcard.model.error.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class StudyDomainException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public StudyDomainException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
