package com.centerton.centerton.domain.patient.exception;

import com.centerton.centerton.global.exception.BaseException;

public class PatientAccessLinkInvalidException extends BaseException {

    public PatientAccessLinkInvalidException() {
        super(PatientErrorCode.PATIENT_ACCESS_LINK_INVALID);
    }
}
