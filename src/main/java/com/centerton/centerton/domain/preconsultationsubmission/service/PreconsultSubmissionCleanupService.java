package com.centerton.centerton.domain.preconsultationsubmission.service;

import com.centerton.centerton.domain.preconsultationsubmission.entity.FileAsset;
import com.centerton.centerton.domain.preconsultationsubmission.entity.PreconsultSubmission;
import com.centerton.centerton.domain.preconsultationsubmission.repository.FileAssetRepository;
import com.centerton.centerton.domain.preconsultationsubmission.repository.PreconsultSubmissionRepository;
import com.centerton.centerton.domain.preconsultationsubmission.storage.PreconsultFileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreconsultSubmissionCleanupService {

    private final PreconsultSubmissionRepository submissionRepository;
    private final FileAssetRepository fileAssetRepository;
    private final PreconsultFileStorage fileStorage;

    public void deleteByAppointmentId(Long appointmentId) {
        submissionRepository
                .findByAppointmentAppointmentId(appointmentId)
                .ifPresent(this::deleteSubmission);
    }

    private void deleteSubmission(PreconsultSubmission submission) {
        List<FileAsset> fileAssets = fileAssetRepository
                .findAllBySubmissionIdOrderByFileIdAsc(
                        submission.getSubmissionId()
                );
        List<String> storedFileNames = fileAssets.stream()
                .map(FileAsset::getFileUrl)
                .filter(fileUrl -> fileUrl.startsWith(
                        PreconsultSubmissionService.FILE_URL_PREFIX
                ))
                .map(fileUrl -> fileUrl.substring(
                        PreconsultSubmissionService.FILE_URL_PREFIX.length()
                ))
                .toList();

        fileAssetRepository.deleteAllInBatch(fileAssets);
        submissionRepository.delete(submission);
        submissionRepository.flush();

        deleteFilesAfterCommit(storedFileNames);
    }

    private void deleteFilesAfterCommit(List<String> storedFileNames) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteFilesQuietly(storedFileNames);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        deleteFilesQuietly(storedFileNames);
                    }
                }
        );
    }

    private void deleteFilesQuietly(List<String> storedFileNames) {
        for (String storedFileName : storedFileNames) {
            try {
                fileStorage.delete(storedFileName);
            } catch (RuntimeException exception) {
                log.error(
                        "예약 취소 후 사전 제출 파일을 삭제하지 못했습니다. storedFileName={}",
                        storedFileName,
                        exception
                );
            }
        }
    }
}
