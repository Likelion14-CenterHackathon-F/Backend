package com.centerton.centerton.domain.patient.exception;

import com.centerton.centerton.global.exception.BaseException;

public class LanguageInvalidException extends BaseException {

    public LanguageInvalidException() {
        super(PatientErrorCode.PATIENT_LANGUAGE_INVALID);
    }
}
