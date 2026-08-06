package com.centerton.centerton.domain.patient.repository;

import com.centerton.centerton.domain.patient.entity.PatientAccessLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientAccessLinkRepository extends JpaRepository<PatientAccessLink, Long> {

    boolean existsByTokenHash(String tokenHash);

    Optional<PatientAccessLink> findByTokenHash(String tokenHash);
}
