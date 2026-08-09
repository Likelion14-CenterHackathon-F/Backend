package com.centerton.centerton.domain.preconsultationsubmission.service;

import com.centerton.centerton.domain.appointment.entity.Appointment;
import com.centerton.centerton.domain.appointment.entity.ReservationSlot;
import com.centerton.centerton.domain.appointment.repository.AppointmentRepository;
import com.centerton.centerton.domain.appointment.repository.ReservationSlotRepository;
import com.centerton.centerton.domain.consultation.repository.ConsultationSessionRepository;
import com.centerton.centerton.domain.preconsultationsubmission.dto.response.PreconsultSubmissionRes;
import com.centerton.centerton.domain.preconsultationsubmission.entity.FileAsset;
import com.centerton.centerton.domain.preconsultationsubmission.entity.PreconsultSubmission;
import com.centerton.centerton.domain.preconsultationsubmission.exception.PreconsultSubmissionErrorCode;
import com.centerton.centerton.domain.preconsultationsubmission.repository.FileAssetRepository;
import com.centerton.centerton.domain.preconsultationsubmission.repository.PreconsultSubmissionRepository;
import com.centerton.centerton.domain.preconsultationsubmission.storage.StoredPreconsultFile;
import com.centerton.centerton.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PreconsultSubmissionTransactionService {

    private final AppointmentRepository appointmentRepository;
    private final ReservationSlotRepository reservationSlotRepository;
    private final ConsultationSessionRepository consultationSessionRepository;
    private final PreconsultSubmissionRepository submissionRepository;
    private final FileAssetRepository fileAssetRepository;

    @Transactional
    public PreconsultSubmissionRes createSubmission(
            Long patientId,
            Long appointmentId,
            String symptomNote,
            List<StoredPreconsultFile> storedFiles
    ) {
        Appointment appointment = getAppointmentForUpdate(
                patientId,
                appointmentId
        );

        validateConsultationNotStarted(appointment);
        validateSubmissionNotExists(appointment.getAppointmentId());

        PreconsultSubmission submission = submissionRepository.saveAndFlush(
                PreconsultSubmission.create(
                        symptomNote,
                        appointment
                )
        );

        List<FileAsset> fileAssets = storedFiles.stream()
                .map(storedFile -> FileAsset.create(
                        PreconsultSubmissionService.FILE_URL_PREFIX
                                + storedFile.storedFileName(),
                        submission.getSubmissionId()
                ))
                .toList();

        return PreconsultSubmissionRes.of(
                submission,
                fileAssetRepository.saveAllAndFlush(fileAssets)
        );
    }

    private Appointment getAppointmentForUpdate(
            Long patientId,
            Long appointmentId
    ) {
        return appointmentRepository
                .findByIdAndPatientIdForUpdate(
                        appointmentId,
                        patientId
                )
                .orElseThrow(() -> new BaseException(
                        PreconsultSubmissionErrorCode.APPOINTMENT_NOT_FOUND
                ));
    }

    private void validateConsultationNotStarted(Appointment appointment) {
        if (consultationSessionRepository.existsByAppointmentId(
                appointment.getAppointmentId()
        )) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.CONSULTATION_ALREADY_STARTED
            );
        }

        ReservationSlot slot = reservationSlotRepository
                .findById(appointment.getSlotId())
                .orElseThrow(() -> new BaseException(
                        PreconsultSubmissionErrorCode.APPOINTMENT_NOT_FOUND
                ));

        if (!slot.getStartsAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.CONSULTATION_ALREADY_STARTED
            );
        }
    }

    private void validateSubmissionNotExists(Long appointmentId) {
        if (submissionRepository.existsByAppointmentAppointmentId(
                appointmentId
        )) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.SUBMISSION_ALREADY_EXISTS
            );
        }
    }
}
