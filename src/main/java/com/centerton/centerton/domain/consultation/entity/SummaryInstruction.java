package com.centerton.centerton.domain.consultation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "SUMMARY_INSTRUCTION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
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
}