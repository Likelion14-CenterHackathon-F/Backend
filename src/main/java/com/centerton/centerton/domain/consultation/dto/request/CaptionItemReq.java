package com.centerton.centerton.domain.consultation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CaptionItemReq(

        @NotBlank
        String sentenceId,

        @NotNull
        @PositiveOrZero
        Integer sequenceNumber,

        @NotBlank
        String speakerAgoraUid,

        @NotBlank
        String sourceLanguage,

        @NotBlank
        String sourceText,

        String targetLanguage,

        String translatedText,

        String textTimestamp,

        @PositiveOrZero
        Integer durationMs,

        boolean isFinal
) {
}
