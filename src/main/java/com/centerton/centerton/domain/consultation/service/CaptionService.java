package com.centerton.centerton.domain.consultation.service;

import com.centerton.centerton.domain.consultation.dto.request.CaptionBatchReq;
import com.centerton.centerton.domain.consultation.dto.request.CaptionItemReq;
import com.centerton.centerton.domain.consultation.dto.response.CaptionBatchRes;
import com.centerton.centerton.domain.consultation.dto.response.CaptionRes;
import com.centerton.centerton.domain.consultation.entity.ConsultationSession;
import com.centerton.centerton.domain.consultation.entity.TranscriptSegment;
import com.centerton.centerton.domain.consultation.entity.enums.ParticipantRole;
import com.centerton.centerton.domain.consultation.exception.ConsultationErrorCode;
import com.centerton.centerton.domain.consultation.repository.ConsultationSessionRepository;
import com.centerton.centerton.domain.consultation.repository.TranscriptSegmentRepository;
import com.centerton.centerton.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CaptionService {

    private final ConsultationSessionRepository sessionRepository;
    private final TranscriptSegmentRepository transcriptRepository;

    public CaptionService(
            ConsultationSessionRepository sessionRepository,
            TranscriptSegmentRepository transcriptRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.transcriptRepository = transcriptRepository;
    }

    @Transactional
    public CaptionBatchRes saveBatch(Long appointmentId, CaptionBatchReq request) {
        ConsultationSession session = getSession(appointmentId);
        validateSessionId(session, request.sessionId());

        int insertedCount = 0;
        int updatedCount = 0;

        for (CaptionItemReq caption : request.captions()) {

            ParticipantRole speakerRole = resolveSpeakerRole(
                    session,
                    caption.speakerAgoraUid()
            );

            Optional<TranscriptSegment> existing = transcriptRepository
                    .findByConsultationSessionSessionIdAndSentenceId(
                            session.getSessionId(),
                            caption.sentenceId()
                    );

            if (existing.isPresent()) {
                existing.get().updateFinalCaption(
                        caption.sourceLanguage(),
                        caption.sourceText(),
                        caption.targetLanguage(),
                        caption.translatedText(),
                        caption.textTimestamp(),
                        caption.durationMs()
                );
                updatedCount++;
                continue;
            }

            TranscriptSegment segment = TranscriptSegment.create(
                    session,
                    caption.sequenceNumber(),
                    speakerRole,
                    caption.speakerAgoraUid(),
                    caption.sourceLanguage(),
                    caption.sourceText(),
                    blankToNull(caption.targetLanguage()),
                    blankToNull(caption.translatedText()),
                    caption.sentenceId(),
                    caption.textTimestamp(),
                    caption.durationMs()
            );

            transcriptRepository.save(segment);
            insertedCount++;
        }

        return new CaptionBatchRes(request.captions().size(), insertedCount, updatedCount);
    }

    public List<CaptionRes> getCaptions(Long appointmentId) {
        ConsultationSession session = getSession(appointmentId);

        return transcriptRepository
                .findAllByConsultationSessionSessionIdOrderBySequenceNumberAsc(
                        session.getSessionId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ConsultationSession getSession(Long appointmentId) {
        return sessionRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new BaseException(
                        ConsultationErrorCode.CONSULTATION_NOT_FOUND
                ));
    }

    private void validateSessionId(
            ConsultationSession session,
            Long requestedSessionId
    ) {
        if (!session.getSessionId().equals(requestedSessionId)) {
            throw new BaseException(ConsultationErrorCode.CONSULTATION_SESSION_MISMATCH);
        }
    }

    private ParticipantRole resolveSpeakerRole(
            ConsultationSession session,
            Integer speakerAgoraUid
    ) {
        try {
            return session.resolveParticipantRole(speakerAgoraUid);
        } catch (IllegalArgumentException exception) {
            throw new BaseException(ConsultationErrorCode.INVALID_CAPTION_SPEAKER);
        }
    }

    private CaptionRes toResponse(TranscriptSegment segment) {
        return new CaptionRes(
                segment.getTranscriptSegmentId(),
                segment.getSentenceId(),
                segment.getSequenceNumber(),
                segment.getSpeakerRole(),
                segment.getSpeakerAgoraUid(),
                segment.getSourceLanguage(),
                segment.getSourceText(),
                segment.getTargetLanguage(),
                segment.getTranslatedText(),
                Boolean.TRUE.equals(segment.getFinalResult()),
                segment.getTextTimestamp(),
                segment.getDurationMs(),
                segment.getCreatedAt()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
