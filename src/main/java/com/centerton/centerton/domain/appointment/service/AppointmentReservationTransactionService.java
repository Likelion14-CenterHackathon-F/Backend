package com.centerton.centerton.domain.appointment.service;

import com.centerton.centerton.domain.aftercare.exception.AftercareErrorCode;
import com.centerton.centerton.domain.aftercare.repository.AftercareCaseRepository;
import com.centerton.centerton.domain.appointment.dto.request.AppointmentCreateReq;
import com.centerton.centerton.domain.appointment.entity.Appointment;
import com.centerton.centerton.domain.appointment.entity.ReservationSlot;
import com.centerton.centerton.domain.appointment.entity.enums.AppointmentStatus;
import com.centerton.centerton.domain.appointment.exception.AppointmentErrorCode;
import com.centerton.centerton.domain.appointment.policy.AppointmentTimePolicy;
import com.centerton.centerton.domain.appointment.repository.AppointmentRepository;
import com.centerton.centerton.domain.appointment.repository.ReservationSlotRepository;
import com.centerton.centerton.domain.preconsultationsubmission.entity.FileAsset;
import com.centerton.centerton.domain.preconsultationsubmission.entity.PreconsultSubmission;
import com.centerton.centerton.domain.preconsultationsubmission.repository.FileAssetRepository;
import com.centerton.centerton.domain.preconsultationsubmission.repository.PreconsultSubmissionRepository;
import com.centerton.centerton.domain.preconsultationsubmission.service.PreconsultSubmissionService;
import com.centerton.centerton.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppointmentReservationTransactionService {

    private final AppointmentRepository appointmentRepository;
    private final ReservationSlotRepository reservationSlotRepository;
    private final AftercareCaseRepository aftercareCaseRepository;
    private final PreconsultSubmissionRepository submissionRepository;
    private final FileAssetRepository fileAssetRepository;

    @Transactional
    public CreatedAppointment createAppointment(
            Long patientId,
            AppointmentCreateReq request,
            PreconsultSubmissionService.PreparedPreconsultSubmission prepared,
            LocalDateTime nowUtc
    ) {
        validateAftercareCaseOwner(request.getCaseId(), patientId);

        ReservationSlot slot = reservationSlotRepository
                .findByIdForUpdate(request.getSlotId())
                .orElseThrow(() -> new BaseException(
                        AppointmentErrorCode.RESERVATION_SLOT_NOT_FOUND
                ));

        validateReservable(slot, nowUtc);

        slot.reserve();
        Appointment appointment = Appointment.create(
                request.getCaseId(),
                patientId,
                slot.getSlotId()
        );

        Appointment savedAppointment = appointmentRepository
                .saveAndFlush(appointment);
        PreconsultSubmission submission = submissionRepository
                .saveAndFlush(PreconsultSubmission.create(
                        prepared.symptomCategory(),
                        prepared.symptomNote(),
                        savedAppointment
                ));

        fileAssetRepository.saveAllAndFlush(
                prepared.storedFiles().stream()
                        .map(storedFile -> FileAsset.create(
                                PreconsultSubmissionService.FILE_URL_PREFIX
                                        + storedFile.storedFileName(),
                                submission.getSubmissionId()
                        ))
                        .toList()
        );

        return new CreatedAppointment(savedAppointment, slot);
    }

    private void validateAftercareCaseOwner(Long caseId, Long patientId) {
        if (!aftercareCaseRepository.existsByCaseIdAndPatientId(
                caseId,
                patientId
        )) {
            throw new BaseException(
                    AftercareErrorCode.AFTERCARE_CASE_NOT_FOUND
            );
        }
    }

    private void validateReservable(
            ReservationSlot slot,
            LocalDateTime nowUtc
    ) {
        if (!slot.hasValidTimeRange()) {
            throw new BaseException(
                    AppointmentErrorCode.RESERVATION_SLOT_UNAVAILABLE
            );
        }

        if (!AppointmentTimePolicy.canReserve(slot.getStartsAt(), nowUtc)) {
            throw new BaseException(
                    AppointmentErrorCode.RESERVATION_DEADLINE_PASSED
            );
        }

        if (!slot.isAvailable()
                || appointmentRepository.existsBySlotIdAndStatus(
                        slot.getSlotId(),
                        AppointmentStatus.CONFIRMED
                )) {
            throw new BaseException(
                    AppointmentErrorCode.RESERVATION_SLOT_ALREADY_RESERVED
            );
        }
    }

    public record CreatedAppointment(
            Appointment appointment,
            ReservationSlot slot
    ) {
    }
}
