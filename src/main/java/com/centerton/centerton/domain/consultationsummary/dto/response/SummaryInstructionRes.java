package com.centerton.centerton.domain.consultationsummary.dto.response;

import com.centerton.centerton.domain.consultationsummary.entity.SummaryInstruction;

import java.time.LocalDateTime;

public record SummaryInstructionRes(
        Long instructionId,
        String content,
        Integer sortOrder,
        Boolean patientCompleted,
        LocalDateTime completedAt
) {

    public static SummaryInstructionRes from(SummaryInstruction instruction) {
        return new SummaryInstructionRes(
                instruction.getInstructionId(),
                instruction.getContent(),
                instruction.getSortOrder(),
                instruction.getPatientCompleted(),
                instruction.getCompletedAt()
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
                instruction.getCompletedAt()
        );
    }
}
