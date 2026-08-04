package com.centerton.centerton.domain.consultation.dto.response;

import java.time.LocalDateTime;

public record CaptionRes(
        Long transcriptSegmentId,
        String sentenceId,
        Integer sequenceNumber,
        String speakerRole,
        String speakerAgoraUid,
        String sourceLanguage,
        String sourceText,
        String targetLanguage,
        String translatedText,
        boolean isFinal,
        String textTimestamp,
        Integer durationMs,
        LocalDateTime createdAt
) {
}
