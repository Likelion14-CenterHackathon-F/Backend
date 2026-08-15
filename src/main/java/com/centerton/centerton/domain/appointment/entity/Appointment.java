package com.centerton.centerton.domain.appointment.entity;

import com.centerton.centerton.domain.appointment.entity.enums.AppointmentCancelReason;
import com.centerton.centerton.domain.appointment.entity.enums.AppointmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "appointments")
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

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20,
            columnDefinition = "varchar(20) default 'CONFIRMED'"
    )
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason", length = 50)
    private AppointmentCancelReason cancelReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public static Appointment create(Long caseId, Long patientId, Long slotId) {
        return new Appointment(
                null,
                caseId,
                patientId,
                slotId,
                AppointmentStatus.CONFIRMED,
                null,
                null
        );
    }

    public void changeSlot(Long slotId) {
        this.slotId = slotId;
    }

    public void cancel(
            AppointmentCancelReason cancelReason,
            LocalDateTime cancelledAt
    ) {
        this.status = AppointmentStatus.CANCELLED;
        this.cancelReason = cancelReason;
        this.cancelledAt = cancelledAt;
    }

    public void complete() {
        if (status == AppointmentStatus.CONFIRMED) {
            status = AppointmentStatus.COMPLETED;
        }
    }

    public boolean isConfirmed() {
        return status == AppointmentStatus.CONFIRMED;
    }

    public boolean isCancelled() {
        return status == AppointmentStatus.CANCELLED;
    }
}
