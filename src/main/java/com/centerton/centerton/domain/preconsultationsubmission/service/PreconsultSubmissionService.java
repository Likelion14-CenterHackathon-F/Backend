package com.centerton.centerton.domain.preconsultationsubmission.service;

import com.centerton.centerton.domain.appointment.repository.AppointmentRepository;
import com.centerton.centerton.domain.preconsultationsubmission.dto.response.PreconsultDownloadFile;
import com.centerton.centerton.domain.preconsultationsubmission.dto.response.PreconsultSubmissionRes;
import com.centerton.centerton.domain.preconsultationsubmission.entity.FileAsset;
import com.centerton.centerton.domain.preconsultationsubmission.entity.PreconsultSubmission;
import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;
import com.centerton.centerton.domain.preconsultationsubmission.exception.PreconsultSubmissionErrorCode;
import com.centerton.centerton.domain.preconsultationsubmission.repository.FileAssetRepository;
import com.centerton.centerton.domain.preconsultationsubmission.repository.PreconsultSubmissionRepository;
import com.centerton.centerton.domain.preconsultationsubmission.storage.PreconsultFileStorage;
import com.centerton.centerton.domain.preconsultationsubmission.storage.PreconsultFileType;
import com.centerton.centerton.domain.preconsultationsubmission.storage.PreconsultFileValidator;
import com.centerton.centerton.domain.preconsultationsubmission.storage.StoredPreconsultFile;
import com.centerton.centerton.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreconsultSubmissionService {

    public static final String FILE_URL_PREFIX = "/api/preconsult-submissions/files/";

    private static final int MAX_SYMPTOM_NOTE_LENGTH = 500;

    private final AppointmentRepository appointmentRepository;
    private final PreconsultSubmissionRepository submissionRepository;
    private final FileAssetRepository fileAssetRepository;
    private final PreconsultFileStorage fileStorage;
    private final PreconsultFileValidator fileValidator;

    public PreparedPreconsultSubmission prepareSubmission(
            Set<SymptomCategory> requestedSymptomCategories,
            String requestedSymptomNote,
            List<MultipartFile> requestedFiles
    ) {
        Set<SymptomCategory> symptomCategories =
                immutableSymptomCategories(requestedSymptomCategories);
        String symptomNote = normalizeSymptomNote(requestedSymptomNote);
        List<MultipartFile> files = resolveFiles(requestedFiles);

        validateSubmissionContent(symptomCategories, symptomNote, files);

        List<StoredPreconsultFile> storedFiles = new ArrayList<>(files.size());

        try {
            for (MultipartFile file : files) {
                storedFiles.add(fileStorage.store(file));
            }

            return new PreparedPreconsultSubmission(
                    symptomCategories,
                    symptomNote,
                    List.copyOf(storedFiles)
            );
        } catch (RuntimeException exception) {
            deleteStoredFilesQuietly(storedFiles);
            throw exception;
        }
    }

    public void cleanupPreparedFiles(
            PreparedPreconsultSubmission submission
    ) {
        deleteStoredFilesQuietly(submission.storedFiles());
    }

    @Transactional(readOnly = true)
    public PreconsultSubmissionRes getSubmission(
            Long patientId,
            Long appointmentId
    ) {
        validateAppointmentOwner(patientId, appointmentId);

        PreconsultSubmission submission = submissionRepository
                .findByAppointmentAppointmentId(appointmentId)
                .orElseThrow(() -> new BaseException(
                        PreconsultSubmissionErrorCode.SUBMISSION_NOT_FOUND
                ));

        List<FileAsset> fileAssets = fileAssetRepository
                .findAllBySubmissionIdOrderByFileIdAsc(
                        submission.getSubmissionId()
                );

        return PreconsultSubmissionRes.of(submission, fileAssets);
    }

    @Transactional(readOnly = true)
    public PreconsultDownloadFile getFile(
            Long patientId,
            String storedFileName
    ) {
        String fileUrl = FILE_URL_PREFIX + storedFileName;
        FileAsset fileAsset = fileAssetRepository
                .findAccessibleFile(fileUrl, patientId)
                .orElseThrow(() -> new BaseException(
                        PreconsultSubmissionErrorCode.FILE_NOT_FOUND
                ));

        PreconsultFileType fileType = fileValidator.resolveStoredFileType(
                storedFileName
        );

        return new PreconsultDownloadFile(
                storedFileName,
                fileType.getResponseContentType(),
                fileStorage.load(extractStoredFileName(fileAsset.getFileUrl()))
        );
    }

    private void validateAppointmentOwner(
            Long patientId,
            Long appointmentId
    ) {
        if (!appointmentRepository.existsByAppointmentIdAndPatientId(
                appointmentId,
                patientId
        )) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.APPOINTMENT_NOT_FOUND
            );
        }
    }

    private String normalizeSymptomNote(String symptomNote) {
        if (symptomNote == null || symptomNote.isBlank()) {
            return null;
        }

        String normalized = symptomNote.strip();

        if (normalized.length() > MAX_SYMPTOM_NOTE_LENGTH) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.SYMPTOM_NOTE_TOO_LONG
            );
        }

        return normalized;
    }

    private List<MultipartFile> resolveFiles(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }

        if (files.size() > PreconsultFileValidator.MAX_FILE_COUNT) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.FILE_COUNT_EXCEEDED
            );
        }

        return List.copyOf(files);
    }

    private void validateSubmissionContent(
            Set<SymptomCategory> symptomCategories,
            String symptomNote,
            List<MultipartFile> files
    ) {
        if (symptomCategories.isEmpty()
                && symptomNote == null
                && files.isEmpty()) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.SUBMISSION_CONTENT_REQUIRED
            );
        }
    }

    private static Set<SymptomCategory> immutableSymptomCategories(
            Set<SymptomCategory> symptomCategories
    ) {
        EnumSet<SymptomCategory> copy =
                EnumSet.noneOf(SymptomCategory.class);
        if (symptomCategories != null) {
            symptomCategories.stream()
                    .filter(java.util.Objects::nonNull)
                    .forEach(copy::add);
        }
        return Collections.unmodifiableSet(copy);
    }

    private void deleteStoredFilesQuietly(
            List<StoredPreconsultFile> storedFiles
    ) {
        for (StoredPreconsultFile storedFile : storedFiles) {
            try {
                fileStorage.delete(storedFile.storedFileName());
            } catch (RuntimeException exception) {
                log.error(
                        "DB에 반영되지 않은 사전 제출 파일을 삭제하지 못했습니다. storedFileName={}",
                        storedFile.storedFileName(),
                        exception
                );
            }
        }
    }

    private String extractStoredFileName(String fileUrl) {
        if (!fileUrl.startsWith(FILE_URL_PREFIX)) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.FILE_NOT_FOUND
            );
        }

        return fileUrl.substring(FILE_URL_PREFIX.length());
    }

    public record PreparedPreconsultSubmission(
            Set<SymptomCategory> symptomCategories,
            String symptomNote,
            List<StoredPreconsultFile> storedFiles
    ) {
        public PreparedPreconsultSubmission {
            symptomCategories = immutableSymptomCategories(symptomCategories);
            storedFiles = List.copyOf(storedFiles);
        }
    }
}
