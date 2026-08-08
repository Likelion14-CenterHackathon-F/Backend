package com.centerton.centerton.domain.consultation.service;

import com.centerton.centerton.domain.appointment.entity.Appointment;
import com.centerton.centerton.domain.appointment.entity.ReservationSlot;
import com.centerton.centerton.domain.appointment.exception.AppointmentErrorCode;
import com.centerton.centerton.domain.appointment.policy.AppointmentTimePolicy;
import com.centerton.centerton.domain.appointment.repository.AppointmentRepository;
import com.centerton.centerton.domain.appointment.repository.ReservationSlotRepository;
import com.centerton.centerton.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class ConsultationJoinPolicy {

    private final AppointmentRepository appointmentRepository;
    private final ReservationSlotRepository reservationSlotRepository;

    public void validateJoin(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BaseException(
                        AppointmentErrorCode.APPOINTMENT_NOT_FOUND
                ));

        ReservationSlot slot = reservationSlotRepository
                .findById(appointment.getSlotId())
                .orElseThrow(() -> new BaseException(
                        AppointmentErrorCode.RESERVATION_SLOT_NOT_FOUND
                ));

        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        if (!AppointmentTimePolicy.canJoin(slot.getStartsAt(), nowUtc)) {
            throw new BaseException(
                    AppointmentErrorCode.APPOINTMENT_JOIN_NOT_ALLOWED
            );
        }
    }
}
