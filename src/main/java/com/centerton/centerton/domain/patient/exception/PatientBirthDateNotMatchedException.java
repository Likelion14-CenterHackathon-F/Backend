package com.centerton.centerton.domain.patient.exception;

import com.centerton.centerton.global.exception.BaseException;

public class PatientBirthDateNotMatchedException extends BaseException {

    public PatientBirthDateNotMatchedException() {
        super(PatientErrorCode.PATIENT_BIRTH_DATE_NOT_MATCHED);
    }
}
