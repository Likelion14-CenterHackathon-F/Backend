package com.centerton.centerton.domain.patient.exception;

import com.centerton.centerton.global.exception.BaseException;

public class PatientAccessLinkTokenGenerationFailedException extends BaseException {

    public PatientAccessLinkTokenGenerationFailedException() {
        super(PatientErrorCode.PATIENT_ACCESS_LINK_TOKEN_GENERATION_FAILED);
    }
}
