package com.centerton.centerton.domain.appointment.service;

import com.centerton.centerton.domain.appointment.dto.request.AppointmentChangeReq;
import com.centerton.centerton.domain.appointment.exception.AppointmentErrorCode;
import com.centerton.centerton.domain.appointment.repository.AppointmentRepository;
import com.centerton.centerton.domain.appointment.repository.ReservationSlotRepository;
import com.centerton.centerton.domain.patient.entity.Patient;
import com.centerton.centerton.domain.patient.repository.PatientRepository;
import com.centerton.centerton.global.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final Long PATIENT_ID = 1L;
    private static final Long OTHER_PATIENT_ID = 2L;
    private static final Long CASE_ID = 10L;
    private static final Long APPOINTMENT_ID = 100L;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ReservationSlotRepository reservationSlotRepository;

    @Mock
    private PatientRepository patientRepository;

    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(
                appointmentRepository,
                reservationSlotRepository,
                patientRepository
        );
    }

    @Test
    void 예약_조회는_인증된_환자의_예약만_검색한다() {
        Patient patient = Patient.builder()
                .id(PATIENT_ID)
                .birthDate(java.time.LocalDate.of(2000, 1, 1))
                .timezoneId("UTC")
                .build();

        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(appointmentRepository.findActiveByPatientIdAndCaseId(
                eq(PATIENT_ID),
                eq(CASE_ID),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        assertThat(appointmentService.getAppointment(PATIENT_ID, CASE_ID).hasAppointment())
                .isFalse();

        verify(appointmentRepository).findActiveByPatientIdAndCaseId(
                eq(PATIENT_ID),
                eq(CASE_ID),
                any(LocalDateTime.class)
        );
    }

    @Test
    void 다른_환자의_예약은_변경할_수_없다() {
        Patient patient = Patient.builder()
                .id(OTHER_PATIENT_ID)
                .birthDate(java.time.LocalDate.of(2000, 1, 1))
                .timezoneId("UTC")
                .build();

        when(patientRepository.findById(OTHER_PATIENT_ID)).thenReturn(Optional.of(patient));
        when(appointmentRepository.findByIdAndPatientIdForUpdate(
                APPOINTMENT_ID,
                OTHER_PATIENT_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.changeAppointment(
                OTHER_PATIENT_ID,
                APPOINTMENT_ID,
                new AppointmentChangeReq(200L)
        ))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getBaseResponseCode())
                                .isEqualTo(AppointmentErrorCode.APPOINTMENT_NOT_FOUND));
    }

    @Test
    void 다른_환자의_예약은_취소할_수_없다() {
        when(appointmentRepository.findByIdAndPatientIdForUpdate(
                APPOINTMENT_ID,
                OTHER_PATIENT_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.cancelAppointment(
                OTHER_PATIENT_ID,
                APPOINTMENT_ID
        ))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getBaseResponseCode())
                                .isEqualTo(AppointmentErrorCode.APPOINTMENT_NOT_FOUND));
    }
}
