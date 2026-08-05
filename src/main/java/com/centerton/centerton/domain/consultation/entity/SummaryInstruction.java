package com.centerton.centerton.domain.consultation.entity;

import com.centerton.centerton.global.entity.BaseEntity;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "SUMMARY_INSTRUCTION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SummaryInstruction extends BaseEntity {

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
