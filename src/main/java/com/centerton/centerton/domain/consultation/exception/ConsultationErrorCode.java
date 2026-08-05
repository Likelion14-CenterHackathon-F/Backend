package com.centerton.centerton.domain.consultation.exception;

import com.centerton.centerton.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ConsultationErrorCode implements BaseResponseCode {

    CONSULTATION_NOT_FOUND(
            "CONSULTATION_404_1",
            404,
            "화상상담 세션을 찾을 수 없습니다."
    ),

    CONSULTATION_ALREADY_COMPLETED(
            "CONSULTATION_409_1",
            409,
            "이미 종료된 화상상담입니다."
    ),

    CONSULTATION_PARTICIPANTS_NOT_READY(
            "CONSULTATION_409_2",
            409,
            "환자와 의료진의 Agora UID 및 언어 정보가 모두 필요합니다."
    ),

    CONSULTATION_SESSION_MISMATCH(
            "CONSULTATION_400_1",
            400,
            "요청한 상담과 세션 정보가 일치하지 않습니다."
    ),

    INVALID_CAPTION_IDENTIFIER(
            "CONSULTATION_400_2",
            400,
            "자막 식별자 또는 발화자 UID 형식이 올바르지 않습니다."
    ),

    INVALID_CAPTION_SPEAKER(
            "CONSULTATION_400_3",
            400,
            "상담에 등록되지 않은 발화자입니다."
    ),

    AGORA_CONFIGURATION_MISSING(
            "CONSULTATION_500_1",
            500,
            "Agora 환경변수가 설정되지 않았습니다."
    ),

    AGORA_TOKEN_ISSUE_FAILED(
            "CONSULTATION_502_1",
            502,
            "Agora RTC 토큰 발급에 실패했습니다."
    ),

    STT_AGENT_START_FAILED(
            "CONSULTATION_502_2",
            502,
            "Agora STT Agent 시작에 실패했습니다."
    );

    private final String code;
    private final int httpStatus;
    private final String message;
}