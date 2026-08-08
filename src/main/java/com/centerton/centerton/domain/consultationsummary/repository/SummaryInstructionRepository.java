package com.centerton.centerton.domain.consultationsummary.repository;

import com.centerton.centerton.domain.consultationsummary.entity.SummaryInstruction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SummaryInstructionRepository
        extends JpaRepository<SummaryInstruction, Long> {

    Optional<SummaryInstruction> findByInstructionIdAndConsultationSummarySummaryId(
            Long instructionId,
            Long summaryId
    );
}
