package com.centerton.centerton.domain.consultation.repository;

import com.centerton.centerton.domain.consultation.entity.TranscriptSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, Long> {

    Optional<TranscriptSegment> findByConsultationSessionSessionIdAndSentenceId(
            Long sessionId,
            Long sentenceId
    );

    List<TranscriptSegment> findAllByConsultationSessionSessionIdOrderBySequenceNumberAsc(
            Long sessionId
    );

    boolean existsByConsultationSessionSessionId(Long sessionId);
}
