package com.centerton.centerton.domain.patient.repository;

import com.centerton.centerton.domain.patient.entity.Patient;
import com.centerton.centerton.domain.patient.entity.enums.PatientRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PatientRepositoryTest {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void patientRoleDefaultsToDefault() {
        Patient savedPatient = patientRepository.saveAndFlush(patient());
        entityManager.clear();

        Patient foundPatient = patientRepository.findById(savedPatient.getId()).orElseThrow();

        assertThat(foundPatient.getRole()).isEqualTo(PatientRole.DEFAULT);
    }

    @Test
    void masterPatientRoleIsPersisted() {
        Patient savedPatient = patientRepository.saveAndFlush(Patient.builder()
                .name("Demo Patient")
                .birthDate(LocalDate.of(2000, 1, 1))
                .role(PatientRole.MASTER)
                .build());
        entityManager.clear();

        Patient foundPatient = patientRepository.findById(savedPatient.getId()).orElseThrow();

        assertThat(foundPatient.getRole()).isEqualTo(PatientRole.MASTER);
    }

    private Patient patient() {
        return Patient.builder()
                .name("Default Patient")
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();
    }
}
