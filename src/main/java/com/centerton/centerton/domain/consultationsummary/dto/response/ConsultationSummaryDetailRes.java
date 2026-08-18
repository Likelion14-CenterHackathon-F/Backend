package com.centerton.centerton.domain.consultationsummary.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record ConsultationSummaryDetailRes(
        Long summaryId,
        OffsetDateTime consultedAt,
        String medicalStaffName,
        Integer actualDurationSeconds,
        String language,
        String translatedSummary,
        String consultationDetails,
        List<SummaryInstructionRes> instructions,
        Long sessionId
) {
}
