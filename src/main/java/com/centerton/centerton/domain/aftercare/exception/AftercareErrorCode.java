package com.centerton.centerton.domain.aftercare.exception;

import com.centerton.centerton.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.centerton.centerton.global.constant.StaticValue.NOT_FOUND;

@Getter
@AllArgsConstructor
public enum AftercareErrorCode implements BaseResponseCode {

    AFTERCARE_CASE_NOT_FOUND("AFTERCARE_404_1", NOT_FOUND, "사후관리 케이스를 찾을 수 없습니다."),
    PROCEDURE_RECORD_NOT_FOUND("AFTERCARE_404_2", NOT_FOUND, "응급 의료 리포트에 표시할 시술 기록을 찾을 수 없습니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
