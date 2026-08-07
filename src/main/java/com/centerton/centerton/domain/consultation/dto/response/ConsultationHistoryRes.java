package com.centerton.centerton.domain.consultation.dto.response;

import java.time.LocalDateTime;

public record ConsultationHistoryRes(
        Long appointmentId,
        Long sessionId,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer actualDurationSeconds,
        boolean hasTranscript
) {
}