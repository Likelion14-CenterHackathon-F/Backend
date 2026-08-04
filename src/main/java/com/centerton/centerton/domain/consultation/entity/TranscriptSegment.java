package com.centerton.centerton.domain.consultation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "TRANSCRIPT_SEGMENT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class TranscriptSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transcript_segment_id")
    private Long transcriptSegmentId;

    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    @Column(name = "speaker_role")
    private String speakerRole;

    @Column(name = "source_language")
    private String sourceLanguage;

    @Column(name = "source_text", columnDefinition = "TEXT")
    private String sourceText;

    @Column(name = "target_language")
    private String targetLanguage;

    @Column(name = "translated_text", columnDefinition = "TEXT")
    private String translatedText;

    @Column(name = "is_final")
    private Boolean isFinal;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "sentence_id")
    private Long sentenceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ConsultationSession consultationSession;
}