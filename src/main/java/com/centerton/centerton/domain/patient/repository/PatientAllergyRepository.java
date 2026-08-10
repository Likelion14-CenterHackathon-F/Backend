package com.centerton.centerton.domain.patient.repository;

import com.centerton.centerton.domain.patient.entity.PatientAllergy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientAllergyRepository extends JpaRepository<PatientAllergy, Long> {

    List<PatientAllergy> findAllByPatientIdOrderByAllergyIdAsc(Long patientId);
}
