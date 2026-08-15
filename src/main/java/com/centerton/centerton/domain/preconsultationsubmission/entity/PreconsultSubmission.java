package com.centerton.centerton.domain.preconsultationsubmission.entity;

import com.centerton.centerton.domain.appointment.entity.Appointment;
import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
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

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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

    @Getter(AccessLevel.NONE)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "preconsult_submission_symptom_category",
            joinColumns = @JoinColumn(
                    name = "submission_id",
                    foreignKey = @ForeignKey(
                            name = "fk_preconsult_submission_symptom_category_submission"
                    )
            ),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_preconsult_submission_symptom_category",
                    columnNames = {"submission_id", "symptom_category"}
            )
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "symptom_category", nullable = false, length = 50)
    private Set<SymptomCategory> symptomCategories;

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
            Set<SymptomCategory> symptomCategories,
            String symptomNote,
            Appointment appointment
    ) {
        return new PreconsultSubmission(
                null,
                symptomNote,
                copySymptomCategories(symptomCategories),
                appointment
        );
    }

    public Set<SymptomCategory> getSymptomCategories() {
        return Collections.unmodifiableSet(
                copySymptomCategories(symptomCategories)
        );
    }

    public List<SymptomCategory> getOrderedSymptomCategories() {
        if (symptomCategories == null || symptomCategories.isEmpty()) {
            return List.of();
        }

        return symptomCategories.stream()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .toList();
    }

    public SymptomCategory getSymptomCategory() {
        return getOrderedSymptomCategories().stream()
                .findFirst()
                .orElse(null);
    }

    public Long getAppointmentId() {
        return appointment.getAppointmentId();
    }

    private static EnumSet<SymptomCategory> copySymptomCategories(
            Set<SymptomCategory> symptomCategories
    ) {
        EnumSet<SymptomCategory> copy =
                EnumSet.noneOf(SymptomCategory.class);
        if (symptomCategories != null) {
            symptomCategories.stream()
                    .filter(java.util.Objects::nonNull)
                    .forEach(copy::add);
        }
        return copy;
    }
}
