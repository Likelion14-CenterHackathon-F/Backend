package com.centerton.centerton.domain.appointment.service;

import com.centerton.centerton.domain.appointment.dto.response.AppointmentHomeSummary;
import com.centerton.centerton.domain.appointment.entity.Appointment;
import com.centerton.centerton.domain.appointment.entity.ReservationSlot;
import com.centerton.centerton.domain.appointment.exception.AppointmentErrorCode;
import com.centerton.centerton.domain.appointment.policy.AppointmentTimePolicy;
import com.centerton.centerton.domain.appointment.repository.AppointmentRepository;
import com.centerton.centerton.domain.appointment.repository.ReservationSlotRepository;
import com.centerton.centerton.global.exception.BaseException;
import com.centerton.centerton.global.util.UtcDateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentQueryService {

    private final AppointmentRepository appointmentRepository;
    private final ReservationSlotRepository reservationSlotRepository;

    public Optional<AppointmentHomeSummary> findHomeAppointment(Long patientId, Long caseId) {
        LocalDateTime nowUtc = nowUtc();

        List<Appointment> activeAppointments = appointmentRepository.findActiveByPatientIdAndCaseId(
                patientId,
                caseId,
                nowUtc.minusMinutes(AppointmentTimePolicy.WAITING_ROOM_CLOSE_AFTER_MINUTES)
        );

        if (activeAppointments.isEmpty()) {
            return Optional.empty();
        }

        Appointment appointment = activeAppointments.getFirst();
        ReservationSlot slot = getSlot(appointment.getSlotId());

        return Optional.of(new AppointmentHomeSummary(
                appointment.getAppointmentId(),
                UtcDateTimeUtils.toUtcOffset(slot.getStartsAt())
        ));
    }

    private ReservationSlot getSlot(Long slotId) {
        return reservationSlotRepository.findById(slotId)
                .orElseThrow(() -> new BaseException(AppointmentErrorCode.RESERVATION_SLOT_NOT_FOUND));
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
