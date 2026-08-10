package com.centerton.centerton.domain.aftercare.repository;

import com.centerton.centerton.domain.aftercare.entity.AftercareCase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AftercareCaseRepository extends JpaRepository<AftercareCase, Long> {

    @EntityGraph(attributePaths = {"patient", "procedureRecord", "recoveryStageGuides"})
    @Query("select aftercareCase from AftercareCase aftercareCase where aftercareCase.patient.id = :patientId")
    Optional<AftercareCase> findDashboardByPatientId(@Param("patientId") Long patientId);

    @EntityGraph(attributePaths = {"patient", "procedureRecord"})
    @Query("select aftercareCase from AftercareCase aftercareCase where aftercareCase.patient.id = :patientId")
    Optional<AftercareCase> findEmergencyReportByPatientId(@Param("patientId") Long patientId);
}
