package com.centerton.centerton.domain.consultation.entity;

import com.centerton.centerton.domain.consultation.entity.enums.ParticipantRole;
import com.centerton.centerton.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "TRANSCRIPT_SEGMENT",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_transcript_session_sentence",
                        columnNames = {"session_id", "sentence_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_transcript_session_sequence",
                        columnList = "session_id, sequence_number"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TranscriptSegment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transcript_segment_id")
    private Long transcriptSegmentId;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "speaker_role", nullable = false, length = 30)
    private String speakerRole;

    @Column(name = "speaker_agora_uid", nullable = false, length = 20)
    private String speakerAgoraUid;

    @Column(name = "source_language", nullable = false, length = 20)
    private String sourceLanguage;

    @Column(name = "source_text", nullable = false, columnDefinition = "TEXT")
    private String sourceText;

    @Column(name = "target_language", length = 20)
    private String targetLanguage;

    @Column(name = "translated_text", columnDefinition = "TEXT")
    private String translatedText;

    @Column(name = "is_final", nullable = false)
    private Boolean finalResult;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime createdAt;

    @Column(name = "sentence_id", nullable = false)
    private Long sentenceId;

    @Column(name = "text_timestamp")
    private Long textTimestamp;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ConsultationSession consultationSession;

    public static TranscriptSegment create(
            ConsultationSession consultationSession,
            Integer sequenceNumber,
            ParticipantRole speakerRole,
            String speakerAgoraUid,
            String sourceLanguage,
            String sourceText,
            String targetLanguage,
            String translatedText,
            Long sentenceId,
            Long textTimestamp,
            Integer durationMs,
            LocalDateTime createdAt
    ) {
        return new TranscriptSegment(
                null,
                sequenceNumber,
                speakerRole.name(),
                speakerAgoraUid,
                sourceLanguage,
                sourceText,
                targetLanguage,
                translatedText,
                true,
                createdAt,
                sentenceId,
                textTimestamp,
                durationMs,
                consultationSession
        );
    }

    public void updateFinalCaption(
            String sourceLanguage,
            String sourceText,
            String targetLanguage,
            String translatedText,
            Long textTimestamp,
            Integer durationMs
    ) {
        if (sourceLanguage != null && !sourceLanguage.isBlank()) {
            this.sourceLanguage = sourceLanguage;
        }

        if (sourceText != null && !sourceText.isBlank()) {
            this.sourceText = sourceText;
        }

        if (targetLanguage != null && !targetLanguage.isBlank()) {
            this.targetLanguage = targetLanguage;
        }

        if (translatedText != null && !translatedText.isBlank()) {
            this.translatedText = translatedText;
        }

        if (textTimestamp != null) {
            this.textTimestamp = textTimestamp;
        }

        if (durationMs != null) {
            this.durationMs = durationMs;
        }

        this.finalResult = true;
    }
}
