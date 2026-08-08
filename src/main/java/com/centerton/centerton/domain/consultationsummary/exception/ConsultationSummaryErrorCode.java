package com.centerton.centerton.domain.consultationsummary.exception;

import com.centerton.centerton.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ConsultationSummaryErrorCode implements BaseResponseCode {

    CONSULTATION_SESSION_NOT_FOUND(
            "CONSULTATION_SUMMARY_404_1",
            404,
            "상담 요약을 생성할 화상상담 세션을 찾을 수 없습니다."
    ),

    CONSULTATION_NOT_COMPLETED(
            "CONSULTATION_SUMMARY_409_1",
            409,
            "상담이 종료된 후 요약을 생성할 수 있습니다."
    ),

    CONSULTATION_TRANSCRIPT_EMPTY(
            "CONSULTATION_SUMMARY_409_2",
            409,
            "요약할 최종 상담 자막이 없습니다."
    ),

    SUMMARY_NOT_FOUND(
            "CONSULTATION_SUMMARY_404_2",
            404,
            "상담 요약을 찾을 수 없습니다."
    ),

    SUMMARY_INSTRUCTION_NOT_FOUND(
            "CONSULTATION_SUMMARY_404_3",
            404,
            "의료진 지시사항을 찾을 수 없습니다."
    ),

    UNSUPPORTED_SUMMARY_LANGUAGE(
            "CONSULTATION_SUMMARY_400_1",
            400,
            "지원하지 않는 상담 요약 언어입니다."
    ),

    GEMINI_CONFIGURATION_MISSING(
            "CONSULTATION_SUMMARY_500_1",
            500,
            "Gemini API 환경변수가 설정되지 않았습니다."
    ),

    GEMINI_SUMMARY_FAILED(
            "CONSULTATION_SUMMARY_502_1",
            502,
            "Gemini 상담 요약 생성에 실패했습니다."
    ),

    DEEPL_CONFIGURATION_MISSING(
            "CONSULTATION_SUMMARY_500_2",
            500,
            "DeepL API 환경변수가 설정되지 않았습니다."
    ),

    DEEPL_TRANSLATION_FAILED(
            "CONSULTATION_SUMMARY_502_2",
            502,
            "상담 요약 번역에 실패했습니다."
    );

    private final String code;
    private final int httpStatus;
    private final String message;
}
