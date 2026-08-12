package com.centerton.centerton.domain.aichat.exception;

import com.centerton.centerton.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.centerton.centerton.global.constant.StaticValue.BAD_REQUEST;
import static com.centerton.centerton.global.constant.StaticValue.FORBIDDEN;
import static com.centerton.centerton.global.constant.StaticValue.INTERNAL_SERVER_ERROR;
import static com.centerton.centerton.global.constant.StaticValue.NOT_FOUND;
import static com.centerton.centerton.global.constant.StaticValue.UNSUPPORTED_MEDIA_TYPE;

@Getter
@AllArgsConstructor
public enum AiChatErrorCode implements BaseResponseCode {

    QUESTION_REQUIRED(
            "AI_CHAT_400_1",
            BAD_REQUEST,
            "증상 문의 내용을 입력해주세요."
    ),

    QUESTION_TOO_LONG(
            "AI_CHAT_400_2",
            BAD_REQUEST,
            "증상 문의 내용은 1000자 이하로 입력해주세요."
    ),

    IMAGE_EMPTY(
            "AI_CHAT_400_3",
            BAD_REQUEST,
            "빈 이미지는 첨부할 수 없습니다."
    ),

    IMAGE_TOO_LARGE(
            "AI_CHAT_413_1",
            413,
            "첨부 이미지는 10MB를 초과할 수 없습니다."
    ),

    IMAGE_TYPE_UNSUPPORTED(
            "AI_CHAT_415_1",
            UNSUPPORTED_MEDIA_TYPE,
            "JPG, PNG, WEBP 형식의 이미지만 첨부할 수 있습니다."
    ),

    CHAT_ROOM_NOT_FOUND(
            "AI_CHAT_404_1",
            NOT_FOUND,
            "채팅방을 찾을 수 없습니다."
    ),

    IMAGE_NOT_FOUND(
            "AI_CHAT_404_2",
            NOT_FOUND,
            "첨부 이미지를 찾을 수 없습니다."
    ),

    CHAT_ROOM_ACCESS_DENIED(
            "AI_CHAT_403_1",
            FORBIDDEN,
            "해당 채팅방에 접근할 수 없습니다."
    ),

    IMAGE_STORAGE_FAILED(
            "AI_CHAT_500_1",
            INTERNAL_SERVER_ERROR,
            "첨부 이미지 저장에 실패했습니다. 잠시 후 다시 시도해주세요."
    ),

    OPENAI_CONFIGURATION_MISSING(
            "AI_CHAT_500_2",
            INTERNAL_SERVER_ERROR,
            "OpenAI API 설정이 누락되었습니다."
    ),

    OPENAI_RESPONSE_FAILED(
            "AI_CHAT_500_3",
            INTERNAL_SERVER_ERROR,
            "AI 응답 생성에 실패했습니다. 잠시 후 다시 시도해주세요."
    );

    private final String code;
    private final int httpStatus;
    private final String message;
}
