package com.centerton.centerton.domain.appointment.repository;

import com.centerton.centerton.domain.appointment.entity.Appointment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appointment from Appointment appointment "
            + "where appointment.appointmentId = :appointmentId "
            + "and appointment.patientId = :patientId")
    Optional<Appointment> findByIdAndPatientIdForUpdate(
            @Param("appointmentId") Long appointmentId,
            @Param("patientId") Long patientId
    );

    @Query("select appointment "
            + "from Appointment appointment, ReservationSlot slot "
            + "where appointment.slotId = slot.slotId "
            + "and appointment.patientId = :patientId "
            + "and appointment.caseId = :caseId "
            + "and slot.startsAt >= :minimumStartsAt "
            + "order by slot.startsAt asc")
    List<Appointment> findActiveByPatientIdAndCaseId(
            @Param("patientId") Long patientId,
            @Param("caseId") Long caseId,
            @Param("minimumStartsAt") LocalDateTime minimumStartsAt
    );

    boolean existsBySlotId(Long slotId);

    boolean existsByAppointmentIdAndPatientId(
            Long appointmentId,
            Long patientId
    );
}
