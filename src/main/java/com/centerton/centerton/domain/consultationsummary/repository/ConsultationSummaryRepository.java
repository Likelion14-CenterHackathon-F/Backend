package com.centerton.centerton.domain.consultationsummary.repository;

import com.centerton.centerton.domain.consultationsummary.entity.ConsultationSummary;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationSummaryRepository
        extends JpaRepository<ConsultationSummary, Long> {

    @EntityGraph(attributePaths = {
            "instructions",
            "consultationSession"
    })
    Optional<ConsultationSummary> findByConsultationSessionSessionId(
            Long sessionId
    );

    @EntityGraph(attributePaths = {
            "instructions",
            "consultationSession"
    })
    Optional<ConsultationSummary> findBySummaryId(
            Long summaryId
    );

    @EntityGraph(attributePaths = "consultationSession")
    List<ConsultationSummary> findAllByOrderByConsultedAtDescSummaryIdDesc();
}