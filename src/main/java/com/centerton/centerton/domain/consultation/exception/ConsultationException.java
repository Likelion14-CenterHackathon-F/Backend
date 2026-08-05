package com.centerton.centerton.domain.consultation.exception;

import com.centerton.centerton.global.exception.BaseException;

public class ConsultationException extends BaseException {

    public ConsultationException(
            ConsultationErrorCode errorCode
    ) {
        super(errorCode);
    }

    public ConsultationException(
            ConsultationErrorCode errorCode,
            Throwable cause
    ) {
        super(errorCode);
        initCause(cause);
    }
}