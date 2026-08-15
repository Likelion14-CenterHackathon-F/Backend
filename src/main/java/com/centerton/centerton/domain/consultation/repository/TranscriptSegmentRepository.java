package com.centerton.centerton.domain.consultation.repository;

import com.centerton.centerton.domain.consultation.entity.TranscriptSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, Long> {

    Optional<TranscriptSegment> findByConsultationSessionSessionIdAndSentenceId(
            Long sessionId,
            Long sentenceId
    );

    List<TranscriptSegment> findAllByConsultationSessionSessionIdOrderBySequenceNumberAsc(
            Long sessionId
    );

    @Query("select distinct segment.consultationSession.sessionId "
            + "from TranscriptSegment segment "
            + "where segment.consultationSession.sessionId in :sessionIds")
    Set<Long> findSessionIdsWithTranscript(
            @Param("sessionIds") Collection<Long> sessionIds
    );
}
