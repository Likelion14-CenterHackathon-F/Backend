package com.centerton.centerton.domain.consultation.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CaptionItemReq(

        @NotNull
        @PositiveOrZero
        Long sentenceId,

        @NotNull
        @PositiveOrZero
        Integer sequenceNumber,

        @NotNull
        @Positive
        Integer speakerAgoraUid,

        @NotBlank
        String sourceLanguage,

        @NotBlank
        String sourceText,

        String targetLanguage,

        String translatedText,

        @PositiveOrZero
        Long textTimestamp,

        @PositiveOrZero
        Integer durationMs,

        @JsonProperty("isFinal")
        @NotNull
        @AssertTrue(message = "최종 확정 자막만 저장할 수 있습니다.")
        Boolean finalResult
) {
}