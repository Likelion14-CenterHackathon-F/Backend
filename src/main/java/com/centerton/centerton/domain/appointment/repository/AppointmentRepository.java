package com.centerton.centerton.domain.appointment.repository;

import com.centerton.centerton.domain.appointment.entity.Appointment;
import com.centerton.centerton.domain.appointment.entity.enums.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appointment from Appointment appointment "
            + "where appointment.appointmentId = :appointmentId "
            + "and appointment.patientId = :patientId")
    Optional<Appointment> findByIdAndPatientIdForUpdate(
            @Param("appointmentId") Long appointmentId,
            @Param("patientId") Long patientId
    );

    Optional<Appointment> findByAppointmentIdAndPatientId(
            Long appointmentId,
            Long patientId
    );

    @Query("select appointment "
            + "from Appointment appointment, ReservationSlot slot "
            + "where appointment.slotId = slot.slotId "
            + "and appointment.patientId = :patientId "
            + "and appointment.caseId = :caseId "
            + "and appointment.status = com.centerton.centerton.domain.appointment.entity.enums.AppointmentStatus.CONFIRMED "
            + "and slot.startsAt >= :minimumStartsAt "
            + "order by slot.startsAt asc")
    List<Appointment> findActiveByPatientIdAndCaseId(
            @Param("patientId") Long patientId,
            @Param("caseId") Long caseId,
            @Param("minimumStartsAt") LocalDateTime minimumStartsAt
    );

    boolean existsBySlotIdAndStatus(
            Long slotId,
            AppointmentStatus status
    );

    @Query("select appointment.slotId from Appointment appointment "
            + "where appointment.slotId in :slotIds "
            + "and appointment.status = :status")
    Set<Long> findOccupiedSlotIds(
            @Param("slotIds") Collection<Long> slotIds,
            @Param("status") AppointmentStatus status
    );

    boolean existsByAppointmentIdAndPatientId(
            Long appointmentId,
            Long patientId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appointment from Appointment appointment "
            + "where appointment.appointmentId = :appointmentId")
    Optional<Appointment> findByIdForUpdate(
            @Param("appointmentId") Long appointmentId
    );

    List<Appointment> findAllByPatientIdOrderByAppointmentIdDesc(
            Long patientId
    );
}
