package com.centerton.centerton.domain.patient.exception;

import com.centerton.centerton.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PatientErrorCode implements BaseResponseCode {

    PATIENT_LANGUAGE_INVALID("PATIENT_400_1", 400, "지원하지 않는 언어입니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
