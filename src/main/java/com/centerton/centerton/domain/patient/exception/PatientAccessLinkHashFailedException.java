package com.centerton.centerton.domain.patient.exception;

import com.centerton.centerton.global.exception.BaseException;

public class PatientAccessLinkHashFailedException extends BaseException {

    public PatientAccessLinkHashFailedException() {
        super(PatientErrorCode.PATIENT_ACCESS_LINK_HASH_FAILED);
    }
}
