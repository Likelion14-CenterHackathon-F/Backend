package com.centerton.centerton.domain.preconsultationsubmission.entity;

import com.centerton.centerton.domain.appointment.entity.Appointment;
import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "preconsult_submission",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_preconsult_submission_appointment",
                        columnNames = "appointment_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PreconsultSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long submissionId;

    @Column(name = "symptom_note", columnDefinition = "TEXT")
    private String symptomNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "symptom_category", length = 50)
    private SymptomCategory symptomCategory;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "appointment_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_preconsult_submission_appointment"
            )
    )
    private Appointment appointment;

    public static PreconsultSubmission create(
            SymptomCategory symptomCategory,
            String symptomNote,
            Appointment appointment
    ) {
        return new PreconsultSubmission(
                null,
                symptomNote,
                symptomCategory,
                appointment
        );
    }

    public Long getAppointmentId() {
        return appointment.getAppointmentId();
    }
}
