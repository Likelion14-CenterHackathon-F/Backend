package com.centerton.centerton.domain.consultationsummary.dto.response;

import java.time.OffsetDateTime;

public record ConsultationSummaryListRes(
        Long summaryId,
        OffsetDateTime consultedAt,
        String medicalStaffName,
        Integer actualDurationSeconds,
        String language,
        String translatedSummary,
        Long sessionId
) {
}
