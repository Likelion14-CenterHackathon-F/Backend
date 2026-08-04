package com.centerton.centerton.domain.consultation.repository;

import com.centerton.centerton.domain.consultation.entity.ConsultationSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConsultationSessionRepository extends JpaRepository<ConsultationSession, Long> {

    Optional<ConsultationSession> findByAppointmentId(Long appointmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from ConsultationSession session where session.appointmentId = :appointmentId")
    Optional<ConsultationSession> findByAppointmentIdForUpdate(
            @Param("appointmentId") Long appointmentId
    );

    List<ConsultationSession> findAllByOrderByStartedAtDesc();
}
