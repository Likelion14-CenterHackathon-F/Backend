package com.centerton.centerton.domain.patient.exception;

import com.centerton.centerton.global.exception.BaseException;

public class PatientNotFoundException extends BaseException {

    public PatientNotFoundException() {
        super(PatientErrorCode.PATIENT_NOT_FOUND);
    }
}
