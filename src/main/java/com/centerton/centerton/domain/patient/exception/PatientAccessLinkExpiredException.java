package com.centerton.centerton.domain.patient.exception;

import com.centerton.centerton.global.exception.BaseException;

public class PatientAccessLinkExpiredException extends BaseException {

    public PatientAccessLinkExpiredException() {
        super(PatientErrorCode.PATIENT_ACCESS_LINK_EXPIRED);
    }
}
