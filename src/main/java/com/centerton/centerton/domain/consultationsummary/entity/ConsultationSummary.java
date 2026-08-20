package com.centerton.centerton.domain.consultationsummary.entity;

import com.centerton.centerton.domain.consultation.entity.ConsultationSession;
import com.centerton.centerton.domain.consultationsummary.entity.enums.InstructionIcon;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "consultation_summaries",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_consultation_summary_session",
                        columnNames = "session_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsultationSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id")
    private Long summaryId;

    @Column(name = "consulted_at")
    private LocalDateTime consultedAt;

    @Column(name = "medical_staff_name")
    private String medicalStaffName;

    @Column(name = "translated_summary", columnDefinition = "TEXT")
    private String translatedSummary;

    @Column(name = "consultation_details", columnDefinition = "TEXT")
    private String consultationDetails;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ConsultationSession consultationSession;

    @OrderBy("sortOrder ASC")
    @OneToMany(
            mappedBy = "consultationSummary",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<SummaryInstruction> instructions = new ArrayList<>();

    private ConsultationSummary(
            LocalDateTime consultedAt,
            String medicalStaffName,
            String translatedSummary,
            String consultationDetails,
            ConsultationSession consultationSession
    ) {
        this.consultedAt = consultedAt;
        this.medicalStaffName = medicalStaffName;
        this.translatedSummary = translatedSummary;
        this.consultationDetails = consultationDetails;
        this.consultationSession = consultationSession;
    }

    public static ConsultationSummary create(
            LocalDateTime consultedAt,
            String medicalStaffName,
            String koreanSummary,
            String koreanConsultationDetails,
            ConsultationSession consultationSession
    ) {
        return new ConsultationSummary(
                consultedAt,
                medicalStaffName,
                koreanSummary,
                koreanConsultationDetails,
                consultationSession
        );
    }

    public void addInstruction(
            String content,
            InstructionIcon icon,
            int sortOrder
    ) {
        instructions.add(
                SummaryInstruction.create(
                        this,
                        content,
                        icon,
                        sortOrder
                )
        );
    }
}