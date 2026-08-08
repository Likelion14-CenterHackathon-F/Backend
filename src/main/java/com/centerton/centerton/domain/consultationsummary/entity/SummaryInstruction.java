package com.centerton.centerton.domain.consultationsummary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "summary_instructions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SummaryInstruction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "instruction_id")
    private Long instructionId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "patient_completed")
    private Boolean patientCompleted;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "summary_id", nullable = false)
    private ConsultationSummary consultationSummary;

    private SummaryInstruction(
            String content,
            Integer sortOrder,
            ConsultationSummary consultationSummary
    ) {
        this.content = content;
        this.sortOrder = sortOrder;
        this.patientCompleted = false;
        this.consultationSummary = consultationSummary;
    }

    public static SummaryInstruction create(
            ConsultationSummary consultationSummary,
            String content,
            Integer sortOrder
    ) {
        return new SummaryInstruction(content, sortOrder, consultationSummary);
    }

    public void changeCompletion(Boolean completed, LocalDateTime changedAt) {
        patientCompleted = completed;
        completedAt = Boolean.TRUE.equals(completed) ? changedAt : null;
    }
}
