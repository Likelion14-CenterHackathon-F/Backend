package com.centerton.centerton.domain.consultationsummary.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ConsultationSummaryDetailRes(
        Long summaryId,
        LocalDateTime consultedAt,
        String hospitalName,
        String medicalStaffName,
        Integer actualDurationSeconds,
        String language,
        String translatedSummary,
        String consultationDetails,
        List<SummaryInstructionRes> instructions,
        Long sessionId
) {
}
