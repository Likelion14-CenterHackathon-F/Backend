package com.centerton.centerton.domain.consultationsummary.dto.response;

import com.centerton.centerton.domain.consultationsummary.entity.SummaryInstruction;
import com.centerton.centerton.global.util.UtcDateTimeUtils;

import java.time.OffsetDateTime;

public record SummaryInstructionRes(
        Long instructionId,
        String content,
        Integer sortOrder,
        Boolean patientCompleted,
        OffsetDateTime completedAt
) {

    public static SummaryInstructionRes from(SummaryInstruction instruction) {
        return new SummaryInstructionRes(
                instruction.getInstructionId(),
                instruction.getContent(),
                instruction.getSortOrder(),
                instruction.getPatientCompleted(),
                UtcDateTimeUtils.toUtcOffset(instruction.getCompletedAt())
        );
    }

    public static SummaryInstructionRes translated(
            SummaryInstruction instruction,
            String translatedContent
    ) {
        return new SummaryInstructionRes(
                instruction.getInstructionId(),
                translatedContent,
                instruction.getSortOrder(),
                instruction.getPatientCompleted(),
                UtcDateTimeUtils.toUtcOffset(instruction.getCompletedAt())
        );
    }
}
