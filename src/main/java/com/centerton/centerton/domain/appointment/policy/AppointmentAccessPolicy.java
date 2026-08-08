package com.centerton.centerton.domain.appointment.policy;

import com.centerton.centerton.domain.appointment.exception.AppointmentErrorCode;
import com.centerton.centerton.domain.appointment.repository.AppointmentRepository;
import com.centerton.centerton.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentAccessPolicy {

    private final AppointmentRepository appointmentRepository;

    public void validateAccess(Long patientId, Long appointmentId) {
        if (!appointmentRepository.existsByAppointmentIdAndPatientId(
                appointmentId,
                patientId
        )) {
            throw new BaseException(AppointmentErrorCode.APPOINTMENT_NOT_FOUND);
        }
    }
}
