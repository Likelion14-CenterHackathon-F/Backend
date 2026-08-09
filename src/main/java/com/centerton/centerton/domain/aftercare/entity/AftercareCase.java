package com.centerton.centerton.domain.aftercare.entity;

import com.centerton.centerton.domain.aftercare.entity.enums.RecoveryStage;
import com.centerton.centerton.domain.patient.entity.Patient;
import com.centerton.centerton.global.entity.BaseEntity;
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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "aftercare_cases",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_aftercare_cases_patient",
                columnNames = "patient_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AftercareCase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "case_id")
    private Long caseId;

    // 사후관리 시작일. 홈/사후관리 화면의 "N일차" 계산 기준.
    @Column(name = "aftercare_start_date", nullable = false)
    private LocalDate aftercareStartDate;

    // 사후관리 종료 예정일. 전체 사후관리 기간과 종료 여부 판단 기준.
    @Column(name = "aftercare_end_date")
    private LocalDate aftercareEndDate;

    // 사후관리 전체 기간. 예: "14일 중 5일차"에서 14일.
    @Column(name = "total_care_days")
    private Integer totalCareDays;

    // 응급실 요약 리포트의 "시술 병원(Clinic Hotline)" 연락처.
    @Column(name = "clinic_phone_number")
    private String clinicPhoneNumber;

    // 응급실 요약 리포트의 "보호자(Guardian)" 연락처.
    @Column(name = "guardian_phone_number")
    private String guardianPhoneNumber;

    // 이 사후관리 케이스가 속한 환자. 환자 한 명당 사후관리 한 건만 연결.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // 응급실 요약 리포트의 "시술 기록" 섹션에 표시할 시술 정보.
    @OneToOne(
            mappedBy = "aftercareCase",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private ProcedureRecord procedureRecord;

    // 회복 초기/중기/안정기의 기간과 안내 문구. 의사가 케이스별로 지정.
    @OrderBy("startDay ASC")
    @OneToMany(
            mappedBy = "aftercareCase",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<RecoveryStageGuide> recoveryStageGuides = new ArrayList<>();

    private AftercareCase(Patient patient, LocalDate aftercareStartDate, Integer totalCareDays) {
        this.patient = patient;
        this.aftercareStartDate = aftercareStartDate;
        this.totalCareDays = totalCareDays;
    }

    public static AftercareCase create(Patient patient, LocalDate aftercareStartDate, Integer totalCareDays) {
        return new AftercareCase(patient, aftercareStartDate, totalCareDays);
    }

    public ProcedureRecord registerProcedureRecord(
            LocalDate procedureDate,
            String procedureName,
            String procedureEnglishName,
            String materials,
            String medications
    ) {
        procedureRecord = ProcedureRecord.create(this, procedureDate, procedureName, procedureEnglishName, materials, medications);
        return procedureRecord;
    }

    public void updateEmergencyContacts(String clinicPhoneNumber, String guardianPhoneNumber) {
        this.clinicPhoneNumber = clinicPhoneNumber;
        this.guardianPhoneNumber = guardianPhoneNumber;
    }

    public void addRecoveryStageGuide(
            RecoveryStage recoveryStage,
            Integer startDay,
            Integer endDay,
            String guideContent
    ) {
        recoveryStageGuides.add(RecoveryStageGuide.create(this, recoveryStage, startDay, endDay, guideContent));
    }

    public RecoveryStage calculateRecoveryStage(LocalDate referenceDate) {
        if (aftercareStartDate == null || referenceDate == null) {
            return null;
        }

        int aftercareDay = (int) ChronoUnit.DAYS.between(aftercareStartDate, referenceDate) + 1;

        return recoveryStageGuides.stream()
                .filter(guide -> guide.includes(aftercareDay))
                .findFirst()
                .map(RecoveryStageGuide::getRecoveryStage)
                .orElse(null);
    }

    public void updateAftercareEndDate(LocalDate aftercareEndDate) {
        this.aftercareEndDate = aftercareEndDate;
    }
}
