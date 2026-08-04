package com.centerton.centerton.domain.consultation.exception;

import lombok.Getter;

@Getter
public class ConsultationException extends RuntimeException {

    private final ConsultationErrorCode errorCode;

    public ConsultationException(ConsultationErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ConsultationException(
            ConsultationErrorCode errorCode,
            Throwable cause
    ) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
