package com.centerton.centerton.domain.preconsultationsubmission.repository;

import com.centerton.centerton.domain.preconsultationsubmission.entity.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {

    List<FileAsset> findAllBySubmissionIdOrderByFileIdAsc(Long submissionId);

    @Query("select fileAsset "
            + "from FileAsset fileAsset, PreconsultSubmission submission, Appointment appointment "
            + "where fileAsset.submissionId = submission.submissionId "
            + "and submission.appointmentId = appointment.appointmentId "
            + "and fileAsset.fileUrl = :fileUrl "
            + "and appointment.patientId = :patientId")
    Optional<FileAsset> findAccessibleFile(
            @Param("fileUrl") String fileUrl,
            @Param("patientId") Long patientId
    );
}
