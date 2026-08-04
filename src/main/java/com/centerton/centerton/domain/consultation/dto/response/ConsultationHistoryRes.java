package com.centerton.centerton.domain.consultation.dto.response;

import java.time.LocalDateTime;

public record ConsultationHistoryRes(
        Long consultationId,
        Long sessionId,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer actualDurationSeconds,
        boolean hasTranscript,
        boolean hasSummary
) {
}
