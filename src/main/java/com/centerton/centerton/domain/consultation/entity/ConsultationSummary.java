package com.centerton.centerton.domain.consultation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "CONSULTATION_SUMMARY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ConsultationSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id")
    private Long summaryId;

    @Column(name = "consulted_at")
    private LocalDateTime consultedAt;

    @Column(name = "hospital_name")
    private String hospitalName;

    @Column(name = "medical_staff_name")
    private String medicalStaffName;

    @Column(name = "translated_summary", columnDefinition = "TEXT")
    private String translatedSummary;

    @Column(name = "consultation_details", columnDefinition = "TEXT")
    private String consultationDetails;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ConsultationSession consultationSession;
}