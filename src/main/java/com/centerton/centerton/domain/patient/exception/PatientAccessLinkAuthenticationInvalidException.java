package com.centerton.centerton.domain.patient.exception;

import com.centerton.centerton.global.exception.BaseException;

public class PatientAccessLinkAuthenticationInvalidException extends BaseException {

    public PatientAccessLinkAuthenticationInvalidException() {
        super(PatientErrorCode.PATIENT_ACCESS_LINK_AUTHENTICATION_INVALID);
    }
}
