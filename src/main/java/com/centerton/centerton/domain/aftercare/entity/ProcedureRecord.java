package com.centerton.centerton.domain.aftercare.entity;

import com.centerton.centerton.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(
        name = "procedure_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_procedure_records_case",
                columnNames = "case_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcedureRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "procedure_record_id")
    private Long procedureRecordId;

    // 응급실 요약 리포트의 "시술일자(Date)"에 표시할 실제 시술 날짜.
    @Column(name = "procedure_date", nullable = false)
    private LocalDate procedureDate;

    // 응급실 요약 리포트의 "시술 명칭(Procedure)"에 표시할 한글 시술명.
    @Column(name = "procedure_name", nullable = false)
    private String procedureName;

    // 현지 의료진에게 함께 보여줄 영문 시술명.
    @Column(name = "procedure_english_name")
    private String procedureEnglishName;

    // 응급실 요약 리포트의 "사용 재료(Materials)"에 표시할 재료 목록.
    @Column(name = "materials", columnDefinition = "TEXT")
    private String materials;

    // 응급실 요약 리포트의 "처방/복용 약물(Medications)"에 표시할 약물 목록.
    @Column(name = "medications", columnDefinition = "TEXT")
    private String medications;

    // 이 시술 기록이 속한 사후관리 케이스.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private AftercareCase aftercareCase;

    private ProcedureRecord(
            AftercareCase aftercareCase,
            LocalDate procedureDate,
            String procedureName,
            String procedureEnglishName,
            String materials,
            String medications
    ) {
        this.aftercareCase = aftercareCase;
        this.procedureDate = procedureDate;
        this.procedureName = procedureName;
        this.procedureEnglishName = procedureEnglishName;
        this.materials = materials;
        this.medications = medications;
    }

    public static ProcedureRecord create(
            AftercareCase aftercareCase,
            LocalDate procedureDate,
            String procedureName,
            String procedureEnglishName,
            String materials,
            String medications
    ) {
        return new ProcedureRecord(
                aftercareCase,
                procedureDate,
                procedureName,
                procedureEnglishName,
                materials,
                medications
        );
    }
}
