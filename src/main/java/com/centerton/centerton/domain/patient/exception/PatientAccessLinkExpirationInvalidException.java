package com.centerton.centerton.domain.patient.exception;

import com.centerton.centerton.global.exception.BaseException;

public class PatientAccessLinkExpirationInvalidException extends BaseException {

    public PatientAccessLinkExpirationInvalidException() {
        super(PatientErrorCode.PATIENT_ACCESS_LINK_EXPIRATION_INVALID);
    }
}
