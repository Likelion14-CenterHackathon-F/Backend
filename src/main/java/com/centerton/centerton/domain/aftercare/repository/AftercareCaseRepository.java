package com.centerton.centerton.domain.aftercare.repository;

import com.centerton.centerton.domain.aftercare.entity.AftercareCase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AftercareCaseRepository extends JpaRepository<AftercareCase, Long> {

    @EntityGraph(attributePaths = {"patient", "procedureRecord"})
    Optional<AftercareCase> findByPatientId(Long patientId);
}
