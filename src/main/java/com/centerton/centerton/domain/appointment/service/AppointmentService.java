package com.centerton.centerton.domain.appointment.service;

import com.centerton.centerton.domain.appointment.dto.request.AppointmentChangeReq;
import com.centerton.centerton.domain.appointment.dto.request.AppointmentCreateReq;
import com.centerton.centerton.domain.appointment.dto.request.AppointmentCancelReq;
import com.centerton.centerton.domain.appointment.dto.response.AppointmentDetailRes;
import com.centerton.centerton.domain.appointment.dto.response.AppointmentInfoRes;
import com.centerton.centerton.domain.appointment.dto.response.AppointmentLookupRes;
import com.centerton.centerton.domain.appointment.dto.response.AvailableDateRes;
import com.centerton.centerton.domain.appointment.dto.response.AvailableSlotListRes;
import com.centerton.centerton.domain.appointment.dto.response.AvailableSlotRes;
import com.centerton.centerton.domain.appointment.entity.Appointment;
import com.centerton.centerton.domain.appointment.entity.ReservationSlot;
import com.centerton.centerton.domain.appointment.entity.enums.AppointmentStatus;
import com.centerton.centerton.domain.appointment.exception.AppointmentErrorCode;
import com.centerton.centerton.domain.appointment.policy.AppointmentTimePolicy;
import com.centerton.centerton.domain.appointment.repository.AppointmentRepository;
import com.centerton.centerton.domain.appointment.repository.ReservationSlotRepository;
import com.centerton.centerton.domain.consultation.repository.ConsultationSessionRepository;
import com.centerton.centerton.domain.patient.exception.PatientErrorCode;
import com.centerton.centerton.domain.patient.repository.PatientRepository;
import com.centerton.centerton.domain.preconsultationsubmission.entity.PreconsultSubmission;
import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;
import com.centerton.centerton.domain.preconsultationsubmission.repository.PreconsultSubmissionRepository;
import com.centerton.centerton.domain.preconsultationsubmission.service.PreconsultSubmissionService;
import com.centerton.centerton.global.exception.BaseException;
import com.centerton.centerton.global.util.UtcDateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    private final AppointmentRepository appointmentRepository;
    private final ReservationSlotRepository reservationSlotRepository;
    private final PatientRepository patientRepository;
    private final ConsultationSessionRepository consultationSessionRepository;
    private final AppointmentReservationTransactionService reservationTransactionService;
    private final PreconsultSubmissionService preconsultSubmissionService;
    private final PreconsultSubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public List<AppointmentLookupRes> getAppointments(
            Long patientId,
            Long caseId
    ) {
        ensurePatientExists(patientId);
        ZoneId zoneId = UTC_ZONE_ID;
        LocalDateTime nowUtc = nowUtc();

        List<Appointment> activeAppointments =
                appointmentRepository.findActiveByPatientIdAndCaseId(
                        patientId,
                        caseId,
                        nowUtc.minusMinutes(
                                AppointmentTimePolicy.WAITING_ROOM_CLOSE_AFTER_MINUTES
                        )
                );

        if (activeAppointments.isEmpty()) {
            return List.of();
        }

        Map<Long, ReservationSlot> slotsById = reservationSlotRepository
                .findAllById(activeAppointments.stream()
                        .map(Appointment::getSlotId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(
                        ReservationSlot::getSlotId,
                        Function.identity()
                ));
        Map<Long, PreconsultSubmission> submissionsByAppointmentId =
                findSubmissionsByAppointmentId(activeAppointments);

        return activeAppointments.stream()
                .map(appointment -> toAppointmentLookup(
                        appointment,
                        requireSlot(
                                appointment.getSlotId(),
                                slotsById
                        ),
                        submissionsByAppointmentId.get(
                                appointment.getAppointmentId()
                        ),
                        zoneId,
                        nowUtc
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentInfoRes getAppointmentInfo(
            Long patientId,
            Long appointmentId
    ) {
        ensurePatientExists(patientId);

        Appointment appointment = appointmentRepository
                .findByAppointmentIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new BaseException(
                        AppointmentErrorCode.APPOINTMENT_NOT_FOUND
                ));
        ReservationSlot slot = getSlot(appointment.getSlotId());
        PreconsultSubmission submission = submissionRepository
                .findByAppointmentAppointmentId(appointmentId)
                .orElse(null);

        return new AppointmentInfoRes(
                appointment.getAppointmentId(),
                UtcDateTimeUtils.toUtcOffset(slot.getStartsAt()),
                UtcDateTimeUtils.toUtcOffset(slot.getEndsAt()),
                submission == null
                        ? List.of()
                        : submission.getOrderedSymptomCategories(),
                submission == null ? null : submission.getSymptomNote()
        );
    }

    private Map<Long, PreconsultSubmission> findSubmissionsByAppointmentId(
            List<Appointment> appointments
    ) {
        return submissionRepository
                .findAllByAppointmentAppointmentIdIn(
                        appointments.stream()
                                .map(Appointment::getAppointmentId)
                                .toList()
                )
                .stream()
                .collect(Collectors.toMap(
                        PreconsultSubmission::getAppointmentId,
                        Function.identity()
                ));
    }

    @Transactional(readOnly = true)
    public List<AvailableDateRes> getAvailableDates(
            Long patientId,
            int year,
            int month
    ) {
        ensurePatientExists(patientId);
        ZoneId zoneId = UTC_ZONE_ID;
        YearMonth requestedMonth = YearMonth.of(year, month);
        LocalDate today = LocalDate.now(zoneId);

        if (requestedMonth.isBefore(YearMonth.from(today))) {
            return List.of();
        }

        LocalDateTime fromUtc = toUtc(
                requestedMonth.atDay(1).atStartOfDay(),
                zoneId
        );
        LocalDateTime toUtc = toUtc(
                requestedMonth.plusMonths(1).atDay(1).atStartOfDay(),
                zoneId
        );
        LocalDateTime nowUtc = nowUtc();

        List<ReservationSlot> slots =
                reservationSlotRepository
                        .findAllByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                                fromUtc,
                                toUtc
                        );
        Set<Long> occupiedSlotIds = findOccupiedSlotIds(slots);

        Map<LocalDate, Integer> availableCounts = new TreeMap<>();

        for (ReservationSlot slot : slots) {
            if (!isReservable(slot, nowUtc, occupiedSlotIds)) {
                continue;
            }

            LocalDate localDate = UtcDateTimeUtils
                    .toUtcOffset(slot.getStartsAt())
                    .toLocalDate();

            if (localDate.isBefore(today)) {
                continue;
            }

            availableCounts.merge(localDate, 1, Integer::sum);
        }

        return availableCounts.entrySet().stream()
                .map(entry -> new AvailableDateRes(
                        entry.getKey(),
                        entry.getValue()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AvailableSlotListRes getAvailableSlots(
            Long patientId,
            LocalDate date
    ) {
        ensurePatientExists(patientId);
        ZoneId zoneId = UTC_ZONE_ID;
        LocalDateTime fromUtc = toUtc(date.atStartOfDay(), zoneId);
        LocalDateTime toUtc = toUtc(date.plusDays(1).atStartOfDay(), zoneId);
        LocalDateTime nowUtc = nowUtc();

        List<ReservationSlot> slots =
                reservationSlotRepository
                        .findAllByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                                fromUtc,
                                toUtc
                        );
        Set<Long> occupiedSlotIds = findOccupiedSlotIds(slots);

        List<AvailableSlotRes> responses = new ArrayList<>(slots.size());
        int availableCount = 0;

        for (ReservationSlot slot : slots) {
            if (!slot.hasValidTimeRange()) {
                continue;
            }

            boolean available = isReservable(
                    slot,
                    nowUtc,
                    occupiedSlotIds
            );

            if (available) {
                availableCount++;
            }

            responses.add(new AvailableSlotRes(
                    slot.getSlotId(),
                    UtcDateTimeUtils.toUtcOffset(slot.getStartsAt()),
                    UtcDateTimeUtils.toUtcOffset(slot.getEndsAt()),
                    available
            ));
        }

        return new AvailableSlotListRes(
                date,
                availableCount,
                zoneId.getId(),
                responses
        );
    }

    public AppointmentDetailRes createAppointment(
            Long patientId,
            AppointmentCreateReq request
    ) {
        PreconsultSubmissionService.PreparedPreconsultSubmission prepared =
                preconsultSubmissionService.prepareSubmission(
                        normalizeSymptomCategories(request),
                        request.getSymptomNote(),
                        request.getFiles()
                );
        LocalDateTime nowUtc = nowUtc();
        try {
            AppointmentReservationTransactionService.CreatedAppointment created =
                    reservationTransactionService.createAppointment(
                            patientId,
                            request,
                            prepared,
                            nowUtc
                    );
            return toAppointmentDetail(
                    created.appointment(),
                    created.slot(),
                    UTC_ZONE_ID,
                    nowUtc
            );
        } catch (RuntimeException exception) {
            preconsultSubmissionService.cleanupPreparedFiles(prepared);
            throw exception;
        }
    }

    @Transactional
    public AppointmentDetailRes changeAppointment(
            Long patientId,
            Long appointmentId,
            AppointmentChangeReq request
    ) {
        ensurePatientExists(patientId);
        ZoneId zoneId = UTC_ZONE_ID;
        LocalDateTime nowUtc = nowUtc();

        Appointment appointment = getAppointmentForUpdate(
                appointmentId,
                patientId
        );
        validateConfirmed(appointment);

        if (appointment.getSlotId().equals(request.slotId())) {
            ReservationSlot currentSlot = getSlot(appointment.getSlotId());
            return toAppointmentDetail(
                    appointment,
                    currentSlot,
                    zoneId,
                    nowUtc
            );
        }

        validateConsultationNotStarted(appointmentId);

        Map<Long, ReservationSlot> lockedSlots = lockSlotsInOrder(
                appointment.getSlotId(),
                request.slotId()
        );

        ReservationSlot currentSlot = lockedSlots.get(appointment.getSlotId());
        ReservationSlot newSlot = lockedSlots.get(request.slotId());

        validateChangeAllowed(currentSlot, nowUtc);
        validateReservable(newSlot, nowUtc);

        if (appointmentRepository.existsBySlotIdAndStatus(
                newSlot.getSlotId(),
                AppointmentStatus.CONFIRMED
        )) {
            throw new BaseException(
                    AppointmentErrorCode.RESERVATION_SLOT_ALREADY_RESERVED
            );
        }

        currentSlot.release();
        newSlot.reserve();
        appointment.changeSlot(newSlot.getSlotId());

        appointmentRepository.flush();

        return toAppointmentDetail(
                appointment,
                newSlot,
                zoneId,
                nowUtc
        );
    }

    @Transactional
    public void cancelAppointment(
            Long patientId,
            Long appointmentId,
            AppointmentCancelReq request
    ) {
        LocalDateTime nowUtc = nowUtc();

        Appointment appointment = getAppointmentForUpdate(
                appointmentId,
                patientId
        );
        validateConfirmed(appointment);
        validateConsultationNotStarted(appointmentId);
        ReservationSlot slot = getSlotForUpdate(appointment.getSlotId());

        validateCancellationAllowed(slot, nowUtc);

        appointment.cancel(request.cancelReason(), nowUtc);
        slot.release();
    }

    private Map<Long, ReservationSlot> lockSlotsInOrder(
            Long firstSlotId,
            Long secondSlotId
    ) {
        long lower = Math.min(firstSlotId, secondSlotId);
        long higher = Math.max(firstSlotId, secondSlotId);

        Map<Long, ReservationSlot> result = new LinkedHashMap<>();

        ReservationSlot lowerSlot = getSlotForUpdate(lower);
        result.put(lower, lowerSlot);

        ReservationSlot higherSlot = getSlotForUpdate(higher);
        result.put(higher, higherSlot);

        return result;
    }

    private Appointment getAppointmentForUpdate(
            Long appointmentId,
            Long patientId
    ) {
        return appointmentRepository
                .findByIdAndPatientIdForUpdate(appointmentId, patientId)
                .orElseThrow(() -> new BaseException(
                        AppointmentErrorCode.APPOINTMENT_NOT_FOUND
                ));
    }

    private ReservationSlot getSlot(Long slotId) {
        return reservationSlotRepository.findById(slotId)
                .orElseThrow(() -> new BaseException(
                        AppointmentErrorCode.RESERVATION_SLOT_NOT_FOUND
                ));
    }

    private ReservationSlot requireSlot(
            Long slotId,
            Map<Long, ReservationSlot> slotsById
    ) {
        ReservationSlot slot = slotsById.get(slotId);
        if (slot == null) {
            throw new BaseException(
                    AppointmentErrorCode.RESERVATION_SLOT_NOT_FOUND
            );
        }
        return slot;
    }

    private ReservationSlot getSlotForUpdate(Long slotId) {
        return reservationSlotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new BaseException(
                        AppointmentErrorCode.RESERVATION_SLOT_NOT_FOUND
                ));
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

        if (!slot.isAvailable()) {
            throw new BaseException(
                    AppointmentErrorCode.RESERVATION_SLOT_ALREADY_RESERVED
            );
        }
    }

    private boolean isReservable(
            ReservationSlot slot,
            LocalDateTime nowUtc
    ) {
        return slot.isAvailable()
                && slot.hasValidTimeRange()
                && AppointmentTimePolicy.canReserve(slot.getStartsAt(), nowUtc);
    }

    private boolean isReservable(
            ReservationSlot slot,
            LocalDateTime nowUtc,
            Set<Long> occupiedSlotIds
    ) {
        return isReservable(slot, nowUtc)
                && !occupiedSlotIds.contains(slot.getSlotId());
    }

    private Set<Long> findOccupiedSlotIds(List<ReservationSlot> slots) {
        if (slots.isEmpty()) {
            return Set.of();
        }

        return appointmentRepository.findOccupiedSlotIds(
                slots.stream()
                        .map(ReservationSlot::getSlotId)
                        .toList(),
                AppointmentStatus.CONFIRMED
        );
    }

    private void validateChangeAllowed(
            ReservationSlot slot,
            LocalDateTime nowUtc
    ) {
        if (!AppointmentTimePolicy.canReserve(slot.getStartsAt(), nowUtc)) {
            throw new BaseException(
                    AppointmentErrorCode.RESERVATION_DEADLINE_PASSED
            );
        }
    }

    private void validateCancellationAllowed(
            ReservationSlot slot,
            LocalDateTime nowUtc
    ) {
        if (!AppointmentTimePolicy.canCancel(slot.getStartsAt(), nowUtc)) {
            throw new BaseException(
                    AppointmentErrorCode.APPOINTMENT_CANCELLATION_DEADLINE_PASSED
            );
        }
    }

    private void validateConfirmed(Appointment appointment) {
        if (appointment.isCancelled()) {
            throw new BaseException(
                    AppointmentErrorCode.APPOINTMENT_ALREADY_CANCELLED
            );
        }

        if (!appointment.isConfirmed()) {
            throw new BaseException(
                    AppointmentErrorCode.APPOINTMENT_ALREADY_STARTED
            );
        }
    }

    private void validateConsultationNotStarted(Long appointmentId) {
        if (consultationSessionRepository.existsByAppointmentId(appointmentId)) {
            throw new BaseException(
                    AppointmentErrorCode.APPOINTMENT_ALREADY_STARTED
            );
        }
    }

    private AppointmentDetailRes toAppointmentDetail(
            Appointment appointment,
            ReservationSlot slot,
            ZoneId zoneId,
            LocalDateTime nowUtc
    ) {
        LocalDateTime waitingRoomOpensAt =
                AppointmentTimePolicy.waitingRoomOpensAt(slot.getStartsAt());
        LocalDateTime waitingRoomClosesAt =
                AppointmentTimePolicy.waitingRoomClosesAt(slot.getStartsAt());

        return new AppointmentDetailRes(
                appointment.getAppointmentId(),
                appointment.getCaseId(),
                appointment.getSlotId(),
                UtcDateTimeUtils.toUtcOffset(slot.getStartsAt()),
                UtcDateTimeUtils.toUtcOffset(slot.getEndsAt()),
                UtcDateTimeUtils.toUtcOffset(waitingRoomOpensAt),
                UtcDateTimeUtils.toUtcOffset(waitingRoomClosesAt),
                AppointmentTimePolicy.canJoin(slot.getStartsAt(), nowUtc),
                zoneId.getId(),
                appointment.getStatus()
        );
    }

    private AppointmentLookupRes toAppointmentLookup(
            Appointment appointment,
            ReservationSlot slot,
            PreconsultSubmission submission,
            ZoneId zoneId,
            LocalDateTime nowUtc
    ) {
        LocalDateTime waitingRoomOpensAt =
                AppointmentTimePolicy.waitingRoomOpensAt(slot.getStartsAt());
        LocalDateTime waitingRoomClosesAt =
                AppointmentTimePolicy.waitingRoomClosesAt(slot.getStartsAt());

        return new AppointmentLookupRes(
                appointment.getAppointmentId(),
                appointment.getCaseId(),
                appointment.getSlotId(),
                UtcDateTimeUtils.toUtcOffset(slot.getStartsAt()),
                UtcDateTimeUtils.toUtcOffset(slot.getEndsAt()),
                submission == null ? null : submission.getSymptomCategory(),
                submission == null
                        ? List.of()
                        : submission.getOrderedSymptomCategories(),
                submission == null ? null : submission.getSymptomNote(),
                appointment.getStatus(),
                UtcDateTimeUtils.toUtcOffset(waitingRoomOpensAt),
                UtcDateTimeUtils.toUtcOffset(waitingRoomClosesAt),
                AppointmentTimePolicy.canJoin(slot.getStartsAt(), nowUtc),
                zoneId.getId()
        );
    }

    private void ensurePatientExists(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new BaseException(PatientErrorCode.PATIENT_NOT_FOUND);
        }
    }

    private Set<SymptomCategory> normalizeSymptomCategories(
            AppointmentCreateReq request
    ) {
        EnumSet<SymptomCategory> symptomCategories =
                EnumSet.noneOf(SymptomCategory.class);

        if (request.getSymptomCategory() != null) {
            symptomCategories.add(request.getSymptomCategory());
        }
        if (request.getSymptomCategories() != null) {
            request.getSymptomCategories().stream()
                    .filter(java.util.Objects::nonNull)
                    .forEach(symptomCategories::add);
        }

        return symptomCategories;
    }

    private LocalDateTime toUtc(
            LocalDateTime localDateTime,
            ZoneId zoneId
    ) {
        return localDateTime
                .atZone(zoneId)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
