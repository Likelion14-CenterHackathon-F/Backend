package com.centerton.centerton.domain.patient.repository;

import com.centerton.centerton.domain.patient.entity.Patient;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select patient from Patient patient where patient.id = :patientId")
    Optional<Patient> findByIdForUpdate(@Param("patientId") Long patientId);
}
