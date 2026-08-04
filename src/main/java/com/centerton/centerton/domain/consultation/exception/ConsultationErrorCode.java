package com.centerton.centerton.domain.consultation.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ConsultationErrorCode {

    CONSULTATION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "CONSULTATION_NOT_FOUND",
            "화상상담 세션을 찾을 수 없습니다."
    ),
    CONSULTATION_ALREADY_COMPLETED(
            HttpStatus.CONFLICT,
            "CONSULTATION_ALREADY_COMPLETED",
            "이미 종료된 화상상담입니다."
    ),
    CONSULTATION_PARTICIPANTS_NOT_READY(
            HttpStatus.CONFLICT,
            "CONSULTATION_PARTICIPANTS_NOT_READY",
            "환자와 의료진의 Agora UID 및 언어 정보가 모두 필요합니다."
    ),
    CONSULTATION_SESSION_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "CONSULTATION_SESSION_MISMATCH",
            "요청한 상담과 세션 정보가 일치하지 않습니다."
    ),
    INVALID_CAPTION_IDENTIFIER(
            HttpStatus.BAD_REQUEST,
            "INVALID_CAPTION_IDENTIFIER",
            "자막 식별자 또는 발화자 UID 형식이 올바르지 않습니다."
    ),
    INVALID_CAPTION_SPEAKER(
            HttpStatus.BAD_REQUEST,
            "INVALID_CAPTION_SPEAKER",
            "상담에 등록되지 않은 발화자입니다."
    ),
    AGORA_CONFIGURATION_MISSING(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "AGORA_CONFIGURATION_MISSING",
            "Agora 환경변수가 설정되지 않았습니다."
    ),
    AGORA_TOKEN_ISSUE_FAILED(
            HttpStatus.BAD_GATEWAY,
            "AGORA_TOKEN_ISSUE_FAILED",
            "Agora RTC 토큰 발급에 실패했습니다."
    ),
    STT_AGENT_START_FAILED(
            HttpStatus.BAD_GATEWAY,
            "STT_AGENT_START_FAILED",
            "Agora STT Agent 시작에 실패했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
