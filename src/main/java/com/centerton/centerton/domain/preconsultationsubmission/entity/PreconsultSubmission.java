package com.centerton.centerton.domain.preconsultationsubmission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "preconsult_submission")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PreconsultSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long submissionId;

    @Column(name = "symptom_note", columnDefinition = "TEXT")
    private String symptomNote;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    public static PreconsultSubmission create(
            String symptomNote,
            Long appointmentId
    ) {
        return new PreconsultSubmission(
                null,
                symptomNote,
                appointmentId
        );
    }
}
