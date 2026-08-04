package com.centerton.centerton.domain.consultation.repository;

import com.centerton.centerton.domain.consultation.entity.ConsultationSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationSummaryRepository extends JpaRepository<ConsultationSummary, Long> {

    boolean existsByConsultationSessionSessionId(Long sessionId);
}
