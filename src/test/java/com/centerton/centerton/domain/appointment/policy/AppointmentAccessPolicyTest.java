package com.centerton.centerton.domain.appointment.policy;

import com.centerton.centerton.domain.appointment.exception.AppointmentErrorCode;
import com.centerton.centerton.domain.appointment.repository.AppointmentRepository;
import com.centerton.centerton.global.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentAccessPolicyTest {

    private static final Long PATIENT_ID = 1L;
    private static final Long APPOINTMENT_ID = 100L;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentAccessPolicy appointmentAccessPolicy;

    @Test
    void 본인의_예약에는_접근할_수_있다() {
        when(appointmentRepository.existsByAppointmentIdAndPatientId(
                APPOINTMENT_ID,
                PATIENT_ID
        )).thenReturn(true);

        assertThatCode(() -> appointmentAccessPolicy.validateAccess(
                PATIENT_ID,
                APPOINTMENT_ID
        )).doesNotThrowAnyException();
    }

    @Test
    void 다른_환자의_예약에는_접근할_수_없다() {
        when(appointmentRepository.existsByAppointmentIdAndPatientId(
                APPOINTMENT_ID,
                PATIENT_ID
        )).thenReturn(false);

        assertThatThrownBy(() -> appointmentAccessPolicy.validateAccess(
                PATIENT_ID,
                APPOINTMENT_ID
        ))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getBaseResponseCode())
                                .isEqualTo(AppointmentErrorCode.APPOINTMENT_NOT_FOUND));
    }
}
