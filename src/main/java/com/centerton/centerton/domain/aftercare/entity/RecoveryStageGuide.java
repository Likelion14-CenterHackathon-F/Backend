package com.centerton.centerton.domain.aftercare.entity;

import com.centerton.centerton.domain.aftercare.entity.enums.RecoveryStage;
import com.centerton.centerton.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "recovery_stage_guides",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recovery_stage_guides_case_stage",
                columnNames = {"case_id", "recovery_stage"}
        ),
        indexes = @Index(name = "idx_recovery_stage_guides_case", columnList = "case_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecoveryStageGuide extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stage_guide_id")
    private Long stageGuideId;

    // 회복 단계 이름. 예: 회복 초기, 회복 중기, 회복 안정기.
    @Enumerated(EnumType.STRING)
    @Column(name = "recovery_stage", nullable = false, length = 20)
    private RecoveryStage recoveryStage;

    // 이 단계가 시작되는 사후관리 일차. 예: 1이면 1일차부터.
    @Column(name = "start_day", nullable = false)
    private Integer startDay;

    // 이 단계가 끝나는 사후관리 일차. null이면 start_day 이후 계속.
    @Column(name = "end_day")
    private Integer endDay;

    // 사후관리 상세 화면에 표시할 단계별 안내 문구.
    @Column(name = "guide_content", columnDefinition = "TEXT")
    private String guideContent;

    // 이 회복 단계 기준이 속한 사후관리 케이스.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private AftercareCase aftercareCase;

    private RecoveryStageGuide(
            AftercareCase aftercareCase,
            RecoveryStage recoveryStage,
            Integer startDay,
            Integer endDay,
            String guideContent
    ) {
        this.aftercareCase = aftercareCase;
        this.recoveryStage = recoveryStage;
        this.startDay = startDay;
        this.endDay = endDay;
        this.guideContent = guideContent;
    }

    public static RecoveryStageGuide create(
            AftercareCase aftercareCase,
            RecoveryStage recoveryStage,
            Integer startDay,
            Integer endDay,
            String guideContent
    ) {
        return new RecoveryStageGuide(
                aftercareCase,
                recoveryStage,
                startDay,
                endDay,
                guideContent
        );
    }

    public boolean includes(int aftercareDay) {
        int normalizedDay = Math.max(aftercareDay, 1);
        return normalizedDay >= startDay && (endDay == null || normalizedDay <= endDay);
    }
}
