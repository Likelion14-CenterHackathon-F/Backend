package com.centerton.centerton.domain.appointment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "appointments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_appointment_slot",
                        columnNames = "slot_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    public static Appointment create(Long caseId, Long patientId, Long slotId) {
        return new Appointment(null, caseId, patientId, slotId);
    }

    public void changeSlot(Long slotId) {
        this.slotId = slotId;
    }
}
