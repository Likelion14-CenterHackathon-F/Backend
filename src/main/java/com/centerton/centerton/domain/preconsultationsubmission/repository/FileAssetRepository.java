package com.centerton.centerton.domain.preconsultationsubmission.repository;

import com.centerton.centerton.domain.preconsultationsubmission.entity.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {

    List<FileAsset> findAllBySubmissionIdOrderByFileIdAsc(Long submissionId);

    Optional<FileAsset> findByFileUrl(String fileUrl);
}
