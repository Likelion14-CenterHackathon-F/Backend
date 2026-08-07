package com.centerton.centerton.domain.consultation.dto.response;

import com.centerton.centerton.domain.consultation.entity.enums.ParticipantRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record CaptionRes(
        Long transcriptSegmentId,
        Long sentenceId,
        Integer sequenceNumber,
        ParticipantRole speakerRole,
        Integer speakerAgoraUid,
        String sourceLanguage,
        String sourceText,
        String targetLanguage,
        String translatedText,
        @JsonProperty("isFinal") boolean finalResult,
        Long textTimestamp,
        Integer durationMs,
        LocalDateTime createdAt
) {
}
