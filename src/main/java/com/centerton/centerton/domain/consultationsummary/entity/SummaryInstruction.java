package com.centerton.centerton.domain.consultationsummary.entity;

import com.centerton.centerton.domain.consultationsummary.entity.enums.InstructionIcon;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    /*
     * 지시사항 성격에 맞는 아이콘.
     *
     * 신규 지시사항은 항상 값이 있고, 맞는 아이콘이 없으면 ETC 가 들어간다.
     * 아이콘 도입 이전에 저장된 행은 비어 있으므로 nullable 이다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "icon", length = 30)
    private InstructionIcon icon;

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
            InstructionIcon icon,
            Integer sortOrder,
            ConsultationSummary consultationSummary
    ) {
        this.content = content;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.patientCompleted = false;
        this.consultationSummary = consultationSummary;
    }

    public static SummaryInstruction create(
            ConsultationSummary consultationSummary,
            String content,
            InstructionIcon icon,
            Integer sortOrder
    ) {
        return new SummaryInstruction(
                content,
                icon,
                sortOrder,
                consultationSummary
        );
    }

    public void changeCompletion(Boolean completed, LocalDateTime changedAt) {
        patientCompleted = completed;
        completedAt = Boolean.TRUE.equals(completed) ? changedAt : null;
    }
}
