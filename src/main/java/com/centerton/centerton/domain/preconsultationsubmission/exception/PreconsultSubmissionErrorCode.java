package com.centerton.centerton.domain.preconsultationsubmission.exception;

import com.centerton.centerton.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PreconsultSubmissionErrorCode implements BaseResponseCode {

    SUBMISSION_CONTENT_REQUIRED(
            "PRECONSULT_SUBMISSION_400_1",
            400,
            "사진·영상 또는 증상 메모 중 하나 이상을 입력해주세요."
    ),

    SYMPTOM_NOTE_TOO_LONG(
            "PRECONSULT_SUBMISSION_400_2",
            400,
            "증상 메모는 500자 이하로 입력해주세요."
    ),

    FILE_EMPTY(
            "PRECONSULT_SUBMISSION_400_3",
            400,
            "빈 파일은 제출할 수 없습니다."
    ),

    FILE_COUNT_EXCEEDED(
            "PRECONSULT_SUBMISSION_400_4",
            400,
            "첨부 파일은 최대 5개까지 제출할 수 있습니다."
    ),

    FILE_TOO_LARGE(
            "PRECONSULT_SUBMISSION_413_1",
            413,
            "첨부 파일은 파일당 50MB를 초과할 수 없습니다."
    ),

    FILE_TYPE_UNSUPPORTED(
            "PRECONSULT_SUBMISSION_415_1",
            415,
            "JPG, PNG, MP4 형식의 파일만 제출할 수 있습니다."
    ),

    APPOINTMENT_NOT_FOUND(
            "PRECONSULT_SUBMISSION_404_1",
            404,
            "사전 자료를 제출할 수 있는 예약을 찾을 수 없습니다."
    ),

    SUBMISSION_NOT_FOUND(
            "PRECONSULT_SUBMISSION_404_2",
            404,
            "사전 제출 자료를 찾을 수 없습니다."
    ),

    FILE_NOT_FOUND(
            "PRECONSULT_SUBMISSION_404_3",
            404,
            "제출 파일을 찾을 수 없습니다."
    ),

    SUBMISSION_ALREADY_EXISTS(
            "PRECONSULT_SUBMISSION_409_1",
            409,
            "해당 예약에는 이미 사전 자료가 제출되었습니다."
    ),

    CONSULTATION_ALREADY_STARTED(
            "PRECONSULT_SUBMISSION_409_2",
            409,
            "이미 시작된 상담에는 사전 자료를 제출할 수 없습니다."
    ),

    FILE_STORAGE_FAILED(
            "PRECONSULT_SUBMISSION_500_1",
            500,
            "첨부 파일 저장에 실패했습니다. 잠시 후 다시 시도해주세요."
    );

    private final String code;
    private final int httpStatus;
    private final String message;
}
