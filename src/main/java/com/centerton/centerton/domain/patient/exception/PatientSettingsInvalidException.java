package com.centerton.centerton.domain.patient.exception;

import com.centerton.centerton.global.exception.BaseException;

public class PatientSettingsInvalidException extends BaseException {

    public PatientSettingsInvalidException() {
        super(PatientErrorCode.PATIENT_SETTINGS_INVALID);
    }
}
