package com.centerton.centerton.domain.patient.entity;

import com.centerton.centerton.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "patient_allergies",
        indexes = @Index(name = "idx_patient_allergies_patient", columnList = "patient_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientAllergy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allergy_id")
    private Long allergyId;

    // 응급실 요약 리포트의 "알레르기 반응(Allergies)"에 표시할 한글 알레르기 항목.
    @Column(name = "allergen_name", nullable = false)
    private String allergenName;

    // 현지 의료진에게 함께 보여줄 영문 알레르기 항목.
    @Column(name = "allergen_english_name")
    private String allergenEnglishName;

    // 이 알레르기 정보가 속한 환자.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    private PatientAllergy(
            Patient patient,
            String allergenName,
            String allergenEnglishName
    ) {
        this.patient = patient;
        this.allergenName = allergenName;
        this.allergenEnglishName = allergenEnglishName;
    }

    public static PatientAllergy create(
            Patient patient,
            String allergenName,
            String allergenEnglishName
    ) {
        return new PatientAllergy(patient, allergenName, allergenEnglishName);
    }
}
