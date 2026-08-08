package com.centerton.centerton.domain.appointment.service;

import com.centerton.centerton.domain.appointment.dto.request.AppointmentChangeReq;
import com.centerton.centerton.domain.appointment.dto.request.AppointmentCreateReq;
import com.centerton.centerton.domain.appointment.dto.response.AppointmentDetailRes;
import com.centerton.centerton.domain.appointment.dto.response.AppointmentLookupRes;
import com.centerton.centerton.domain.appointment.dto.response.AvailableDateRes;
import com.centerton.centerton.domain.appointment.dto.response.AvailableSlotListRes;
import com.centerton.centerton.domain.appointment.dto.response.AvailableSlotRes;
import com.centerton.centerton.domain.appointment.entity.Appointment;
import com.centerton.centerton.domain.appointment.entity.ReservationSlot;
import com.centerton.centerton.domain.appointment.exception.AppointmentErrorCode;
import com.centerton.centerton.domain.appointment.policy.AppointmentTimePolicy;
import com.centerton.centerton.domain.appointment.repository.AppointmentRepository;
import com.centerton.centerton.domain.appointment.repository.ReservationSlotRepository;
import com.centerton.centerton.domain.patient.entity.Patient;
import com.centerton.centerton.domain.patient.exception.PatientErrorCode;
import com.centerton.centerton.domain.patient.repository.PatientRepository;
import com.centerton.centerton.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ReservationSlotRepository reservationSlotRepository;
    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public AppointmentLookupRes getAppointment(
            Long patientId,
            Long caseId
    ) {
        ZoneId zoneId = getPatientZoneId(patientId);
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
            return AppointmentLookupRes.none();
        }

        Appointment appointment = activeAppointments.getFirst();
        ReservationSlot slot = getSlot(appointment.getSlotId());

        return AppointmentLookupRes.of(
                toAppointmentDetail(appointment, slot, zoneId, nowUtc)
        );
    }

    @Transactional(readOnly = true)
    public List<AvailableDateRes> getAvailableDates(
            Long patientId,
            int year,
            int month
    ) {
        ZoneId zoneId = getPatientZoneId(patientId);
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

        Map<LocalDate, Integer> availableCounts = new TreeMap<>();

        for (ReservationSlot slot : slots) {
            if (!isReservable(slot, nowUtc)) {
                continue;
            }

            LocalDate localDate = toUserTime(slot.getStartsAt(), zoneId).toLocalDate();

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
        ZoneId zoneId = getPatientZoneId(patientId);
        LocalDateTime fromUtc = toUtc(date.atStartOfDay(), zoneId);
        LocalDateTime toUtc = toUtc(date.plusDays(1).atStartOfDay(), zoneId);
        LocalDateTime nowUtc = nowUtc();

        List<ReservationSlot> slots =
                reservationSlotRepository
                        .findAllByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                                fromUtc,
                                toUtc
                        );

        List<AvailableSlotRes> responses = new ArrayList<>(slots.size());
        int availableCount = 0;

        for (ReservationSlot slot : slots) {
            boolean available = isReservable(slot, nowUtc);

            if (available) {
                availableCount++;
            }

            responses.add(new AvailableSlotRes(
                    slot.getSlotId(),
                    toUserTime(slot.getStartsAt(), zoneId),
                    toUserTime(slot.getEndsAt(), zoneId),
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

    @Transactional
    public AppointmentDetailRes createAppointment(
            Long patientId,
            AppointmentCreateReq request
    ) {
        Patient patient = getPatientForUpdate(patientId);
        ZoneId zoneId = resolvePatientZoneId(patient);
        LocalDateTime nowUtc = nowUtc();

        ensureNoActiveAppointment(patientId, request.caseId(), nowUtc);

        ReservationSlot slot = getSlotForUpdate(request.slotId());
        validateReservable(slot, nowUtc);

        if (appointmentRepository.existsBySlotId(slot.getSlotId())) {
            throw new BaseException(
                    AppointmentErrorCode.RESERVATION_SLOT_UNAVAILABLE
            );
        }

        slot.reserve();
        Appointment appointment = Appointment.create(
                request.caseId(),
                patientId,
                request.slotId()
        );

        try {
            Appointment saved = appointmentRepository.saveAndFlush(appointment);
            return toAppointmentDetail(saved, slot, zoneId, nowUtc);
        } catch (DataIntegrityViolationException exception) {
            throw new BaseException(
                    AppointmentErrorCode.RESERVATION_SLOT_UNAVAILABLE
            );
        }
    }

    @Transactional
    public AppointmentDetailRes changeAppointment(
            Long patientId,
            Long appointmentId,
            AppointmentChangeReq request
    ) {
        ZoneId zoneId = getPatientZoneId(patientId);
        LocalDateTime nowUtc = nowUtc();

        Appointment appointment = getAppointmentForUpdate(
                appointmentId,
                patientId
        );

        if (appointment.getSlotId().equals(request.slotId())) {
            ReservationSlot currentSlot = getSlot(appointment.getSlotId());
            return toAppointmentDetail(
                    appointment,
                    currentSlot,
                    zoneId,
                    nowUtc
            );
        }

        Map<Long, ReservationSlot> lockedSlots = lockSlotsInOrder(
                appointment.getSlotId(),
                request.slotId()
        );

        ReservationSlot currentSlot = lockedSlots.get(appointment.getSlotId());
        ReservationSlot newSlot = lockedSlots.get(request.slotId());

        validateChangeOrCancelAllowed(currentSlot, nowUtc);
        validateReservable(newSlot, nowUtc);

        if (appointmentRepository.existsBySlotId(newSlot.getSlotId())) {
            throw new BaseException(
                    AppointmentErrorCode.RESERVATION_SLOT_UNAVAILABLE
            );
        }

        currentSlot.release();
        newSlot.reserve();
        appointment.changeSlot(newSlot.getSlotId());

        try {
            appointmentRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new BaseException(
                    AppointmentErrorCode.RESERVATION_SLOT_UNAVAILABLE
            );
        }

        return toAppointmentDetail(
                appointment,
                newSlot,
                zoneId,
                nowUtc
        );
    }

    @Transactional
    public void cancelAppointment(Long patientId, Long appointmentId) {
        LocalDateTime nowUtc = nowUtc();

        Appointment appointment = getAppointmentForUpdate(
                appointmentId,
                patientId
        );
        ReservationSlot slot = getSlotForUpdate(appointment.getSlotId());

        validateChangeOrCancelAllowed(slot, nowUtc);

        appointmentRepository.delete(appointment);
        slot.release();
    }

    private void ensureNoActiveAppointment(
            Long patientId,
            Long caseId,
            LocalDateTime nowUtc
    ) {
        List<Appointment> activeAppointments =
                appointmentRepository.findActiveByPatientIdAndCaseId(
                        patientId,
                        caseId,
                        nowUtc.minusMinutes(
                                AppointmentTimePolicy.WAITING_ROOM_CLOSE_AFTER_MINUTES
                        )
                );

        if (!activeAppointments.isEmpty()) {
            throw new BaseException(
                    AppointmentErrorCode.ACTIVE_APPOINTMENT_ALREADY_EXISTS
            );
        }
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
        if (!isReservable(slot, nowUtc)) {
            throw new BaseException(
                    AppointmentErrorCode.RESERVATION_SLOT_UNAVAILABLE
            );
        }
    }

    private boolean isReservable(
            ReservationSlot slot,
            LocalDateTime nowUtc
    ) {
        return slot.isAvailable()
                && slot.getStartsAt() != null
                && slot.getStartsAt().isAfter(nowUtc);
    }

    private void validateChangeOrCancelAllowed(
            ReservationSlot slot,
            LocalDateTime nowUtc
    ) {
        if (slot.getStartsAt() == null
                || !slot.getStartsAt().isAfter(nowUtc)) {
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
                toUserTime(slot.getStartsAt(), zoneId),
                toUserTime(slot.getEndsAt(), zoneId),
                toUserTime(waitingRoomOpensAt, zoneId),
                toUserTime(waitingRoomClosesAt, zoneId),
                AppointmentTimePolicy.canJoin(slot.getStartsAt(), nowUtc),
                zoneId.getId()
        );
    }

    private ZoneId getPatientZoneId(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new BaseException(
                        PatientErrorCode.PATIENT_NOT_FOUND
                ));

        return resolvePatientZoneId(patient);
    }

    private Patient getPatientForUpdate(Long patientId) {
        return patientRepository.findByIdForUpdate(patientId)
                .orElseThrow(() -> new BaseException(
                        PatientErrorCode.PATIENT_NOT_FOUND
                ));
    }

    private ZoneId resolvePatientZoneId(Patient patient) {
        String timezoneId = patient.getTimezoneId();

        if (timezoneId == null || timezoneId.isBlank()) {
            return ZoneOffset.UTC;
        }

        try {
            return ZoneId.of(timezoneId);
        } catch (ZoneRulesException exception) {
            return ZoneOffset.UTC;
        }
    }

    private OffsetDateTime toUserTime(
            LocalDateTime utcDateTime,
            ZoneId zoneId
    ) {
        return utcDateTime
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(zoneId)
                .toOffsetDateTime();
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
