package com.centerton.centerton.domain.consultationsummary.dto.response;

import java.time.LocalDateTime;

public record ConsultationSummaryListRes(
        Long summaryId,
        LocalDateTime consultedAt,
        String medicalStaffName,
        Integer actualDurationSeconds,
        String language,
        String translatedSummary,
        Long sessionId
) {
}