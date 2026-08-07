package com.centerton.centerton.domain.patient.exception;

import com.centerton.centerton.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.centerton.centerton.global.constant.StaticValue.BAD_REQUEST;
import static com.centerton.centerton.global.constant.StaticValue.INTERNAL_SERVER_ERROR;
import static com.centerton.centerton.global.constant.StaticValue.NOT_FOUND;
import static com.centerton.centerton.global.constant.StaticValue.UNAUTHORIZED;

@Getter
@AllArgsConstructor
public enum PatientErrorCode implements BaseResponseCode {

    PATIENT_LANGUAGE_INVALID("PATIENT_400_1", BAD_REQUEST, "지원하지 않는 언어입니다."),
    PATIENT_ACCESS_LINK_EXPIRATION_INVALID("PATIENT_400_2", BAD_REQUEST, "매직링크 만료 시간은 1분 이상 30일 이하이어야 합니다."),
    PATIENT_ACCESS_LINK_AUTHENTICATION_INVALID("PATIENT_400_3", BAD_REQUEST, "매직링크 인증 요청값이 올바르지 않습니다."),
    PATIENT_SETTINGS_INVALID("PATIENT_400_4", BAD_REQUEST, "환자 설정 요청값이 올바르지 않습니다."),
    PATIENT_NOT_FOUND("PATIENT_404_1", NOT_FOUND, "존재하지 않는 환자입니다."),
    PATIENT_ACCESS_LINK_INVALID("PATIENT_401_1", UNAUTHORIZED, "유효하지 않은 매직링크입니다."),
    PATIENT_ACCESS_LINK_EXPIRED("PATIENT_401_2", UNAUTHORIZED, "만료된 매직링크입니다."),
    PATIENT_BIRTH_DATE_NOT_MATCHED("PATIENT_401_3", UNAUTHORIZED, "생년월일이 일치하지 않습니다."),
    PATIENT_ACCESS_LINK_TOKEN_GENERATION_FAILED("PATIENT_500_1", INTERNAL_SERVER_ERROR, "매직링크 토큰 생성에 실패했습니다. 잠시 후 다시 시도해주세요."),
    PATIENT_ACCESS_LINK_HASH_FAILED("PATIENT_500_2", INTERNAL_SERVER_ERROR, "매직링크 토큰 보안 처리에 실패했습니다. 잠시 후 다시 시도해주세요.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
