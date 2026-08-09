package com.centerton.centerton.domain.preconsultationsubmission.repository;

import com.centerton.centerton.domain.preconsultationsubmission.entity.PreconsultSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreconsultSubmissionRepository extends JpaRepository<PreconsultSubmission, Long> {

    boolean existsByAppointmentAppointmentId(Long appointmentId);

    Optional<PreconsultSubmission> findByAppointmentAppointmentId(Long appointmentId);
}
