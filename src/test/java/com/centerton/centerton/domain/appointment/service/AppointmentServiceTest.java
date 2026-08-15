package com.centerton.centerton.domain.appointment.service;

import com.centerton.centerton.domain.appointment.dto.request.AppointmentCreateReq;
import com.centerton.centerton.domain.appointment.dto.response.AppointmentInfoRes;
import com.centerton.centerton.domain.appointment.entity.Appointment;
import com.centerton.centerton.domain.appointment.entity.ReservationSlot;
import com.centerton.centerton.domain.appointment.exception.AppointmentErrorCode;
import com.centerton.centerton.domain.appointment.repository.AppointmentRepository;
import com.centerton.centerton.domain.appointment.repository.ReservationSlotRepository;
import com.centerton.centerton.domain.consultation.repository.ConsultationSessionRepository;
import com.centerton.centerton.domain.patient.repository.PatientRepository;
import com.centerton.centerton.domain.preconsultationsubmission.entity.PreconsultSubmission;
import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;
import com.centerton.centerton.domain.preconsultationsubmission.repository.PreconsultSubmissionRepository;
import com.centerton.centerton.domain.preconsultationsubmission.service.PreconsultSubmissionService;
import com.centerton.centerton.global.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final Long PATIENT_ID = 1L;
    private static final Long APPOINTMENT_ID = 101L;
    private static final Long SLOT_ID = 201L;
    private static final LocalDateTime STARTS_AT = LocalDateTime.of(
            2026, 8, 20, 5, 0
    );
    private static final LocalDateTime ENDS_AT = STARTS_AT.plusMinutes(15);

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private ReservationSlotRepository reservationSlotRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private ConsultationSessionRepository consultationSessionRepository;
    @Mock
    private AppointmentReservationTransactionService reservationTransactionService;
    @Mock
    private PreconsultSubmissionService preconsultSubmissionService;
    @Mock
    private PreconsultSubmissionRepository submissionRepository;

    @InjectMocks
    private AppointmentService service;

    @BeforeEach
    void prepareCreationCollaborators() {
        lenient().when(preconsultSubmissionService.prepareSubmission(
                anySet(),
                nullable(String.class),
                anyList()
        )).thenAnswer(invocation ->
                new PreconsultSubmissionService.PreparedPreconsultSubmission(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        List.of()
                ));
        lenient().when(reservationTransactionService.createAppointment(
                anyLong(),
                any(AppointmentCreateReq.class),
                any(PreconsultSubmissionService.PreparedPreconsultSubmission.class),
                any(LocalDateTime.class)
        )).thenReturn(new AppointmentReservationTransactionService.CreatedAppointment(
                Appointment.create(1L, PATIENT_ID, SLOT_ID),
                ReservationSlot.create(STARTS_AT, ENDS_AT)
        ));
    }

    @Test
    void legacySingleSymptomCategoryIsNormalized() {
        AppointmentCreateReq request = request();
        request.setSymptomCategory(SymptomCategory.SWELLING);

        assertNormalizedCategories(request, SymptomCategory.SWELLING);
    }

    @Test
    void newSingleSymptomCategoryIsNormalized() {
        AppointmentCreateReq request = request();
        request.setSymptomCategories(List.of(SymptomCategory.SWELLING));

        assertNormalizedCategories(request, SymptomCategory.SWELLING);
    }

    @Test
    void multipleSymptomCategoriesAreNormalized() {
        AppointmentCreateReq request = request();
        request.setSymptomCategories(List.of(
                SymptomCategory.SWELLING,
                SymptomCategory.BRUISING
        ));

        assertNormalizedCategories(
                request,
                SymptomCategory.SWELLING,
                SymptomCategory.BRUISING
        );
    }

    @Test
    void legacyAndNewSymptomCategoriesAreMerged() {
        AppointmentCreateReq request = request();
        request.setSymptomCategory(SymptomCategory.SWELLING);
        request.setSymptomCategories(List.of(SymptomCategory.BRUISING));

        assertNormalizedCategories(
                request,
                SymptomCategory.SWELLING,
                SymptomCategory.BRUISING
        );
    }

    @Test
    void duplicateAndNullSymptomCategoriesAreRemoved() {
        AppointmentCreateReq request = request();
        request.setSymptomCategory(SymptomCategory.SWELLING);
        List<SymptomCategory> categories = new ArrayList<>();
        categories.add(SymptomCategory.SWELLING);
        categories.add(null);
        categories.add(SymptomCategory.BRUISING);
        request.setSymptomCategories(categories);

        assertNormalizedCategories(
                request,
                SymptomCategory.SWELLING,
                SymptomCategory.BRUISING
        );
    }

    @Test
    void noteOnlyRequestKeepsEmptySymptomCategories() {
        AppointmentCreateReq request = request();
        request.setSymptomNote("저녁에 붓기가 심해집니다.");

        assertNormalizedCategories(request);
    }

    @Test
    void getAppointmentInfoReturnsSingleSymptomCategory() {
        Appointment appointment = ownedAppointmentFixture();
        PreconsultSubmission submission = PreconsultSubmission.create(
                Set.of(SymptomCategory.SWELLING),
                "붓기가 있습니다.",
                appointment
        );
        when(submissionRepository.findByAppointmentAppointmentId(APPOINTMENT_ID))
                .thenReturn(Optional.of(submission));

        AppointmentInfoRes response = service.getAppointmentInfo(
                PATIENT_ID,
                APPOINTMENT_ID
        );

        assertThat(response.symptomCategories())
                .containsExactly(SymptomCategory.SWELLING);
    }

    @Test
    void getAppointmentInfoReturnsMultipleSymptomsInEnumOrder() {
        Appointment appointment = ownedAppointmentFixture();
        PreconsultSubmission submission = PreconsultSubmission.create(
                Set.of(
                        SymptomCategory.BRUISING,
                        SymptomCategory.SWELLING
                ),
                "붓기와 멍",
                appointment
        );
        when(submissionRepository.findByAppointmentAppointmentId(APPOINTMENT_ID))
                .thenReturn(Optional.of(submission));

        AppointmentInfoRes response = service.getAppointmentInfo(
                PATIENT_ID,
                APPOINTMENT_ID
        );

        assertThat(response.symptomCategories()).containsExactly(
                SymptomCategory.SWELLING,
                SymptomCategory.BRUISING
        );
        assertThat(response.startsAt()).isEqualTo(
                STARTS_AT.atOffset(ZoneOffset.UTC)
        );
        assertThat(response.endsAt()).isEqualTo(
                ENDS_AT.atOffset(ZoneOffset.UTC)
        );
        assertThat(response.symptomNote()).isEqualTo("붓기와 멍");
    }

    @Test
    void nonexistentAppointmentIsHiddenAsNotFound() {
        when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);
        when(appointmentRepository.findByAppointmentIdAndPatientId(
                APPOINTMENT_ID,
                PATIENT_ID
        )).thenReturn(Optional.empty());

        assertAppointmentError(AppointmentErrorCode.APPOINTMENT_NOT_FOUND);
    }

    @Test
    void anotherPatientsAppointmentIsHiddenAsNotFound() {
        when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);
        when(appointmentRepository.findByAppointmentIdAndPatientId(
                APPOINTMENT_ID,
                PATIENT_ID
        )).thenReturn(Optional.empty());

        assertAppointmentError(AppointmentErrorCode.APPOINTMENT_NOT_FOUND);
        verify(appointmentRepository).findByAppointmentIdAndPatientId(
                APPOINTMENT_ID,
                PATIENT_ID
        );
    }

    @Test
    void missingReservationSlotReturnsNotFound() {
        when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);
        Appointment appointment = mockAppointment();
        when(appointmentRepository.findByAppointmentIdAndPatientId(
                APPOINTMENT_ID,
                PATIENT_ID
        )).thenReturn(Optional.of(appointment));
        when(reservationSlotRepository.findById(SLOT_ID))
                .thenReturn(Optional.empty());

        assertAppointmentError(
                AppointmentErrorCode.RESERVATION_SLOT_NOT_FOUND
        );
    }

    @Test
    void legacyAppointmentWithoutSubmissionReturnsEmptySymptoms() {
        ownedAppointmentFixture();
        when(submissionRepository.findByAppointmentAppointmentId(APPOINTMENT_ID))
                .thenReturn(Optional.empty());

        AppointmentInfoRes response = service.getAppointmentInfo(
                PATIENT_ID,
                APPOINTMENT_ID
        );

        assertThat(response.symptomCategories()).isEmpty();
        assertThat(response.symptomNote()).isNull();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void assertNormalizedCategories(
            AppointmentCreateReq request,
            SymptomCategory... expected
    ) {
        service.createAppointment(PATIENT_ID, request);

        ArgumentCaptor<Set<SymptomCategory>> captor =
                (ArgumentCaptor) ArgumentCaptor.forClass(Set.class);
        verify(preconsultSubmissionService).prepareSubmission(
                captor.capture(),
                nullable(String.class),
                anyList()
        );
        assertThat(captor.getValue()).containsExactly(expected);
    }

    private AppointmentCreateReq request() {
        AppointmentCreateReq request = new AppointmentCreateReq();
        request.setCaseId(1L);
        request.setSlotId(SLOT_ID);
        return request;
    }

    private Appointment ownedAppointmentFixture() {
        when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);
        Appointment appointment = mockAppointment();
        when(appointmentRepository.findByAppointmentIdAndPatientId(
                APPOINTMENT_ID,
                PATIENT_ID
        )).thenReturn(Optional.of(appointment));

        ReservationSlot slot = org.mockito.Mockito.mock(ReservationSlot.class);
        when(slot.getStartsAt()).thenReturn(STARTS_AT);
        when(slot.getEndsAt()).thenReturn(ENDS_AT);
        when(reservationSlotRepository.findById(SLOT_ID))
                .thenReturn(Optional.of(slot));

        return appointment;
    }

    private Appointment mockAppointment() {
        Appointment appointment = org.mockito.Mockito.mock(Appointment.class);
        lenient().when(appointment.getAppointmentId())
                .thenReturn(APPOINTMENT_ID);
        when(appointment.getSlotId()).thenReturn(SLOT_ID);
        return appointment;
    }

    private void assertAppointmentError(AppointmentErrorCode expected) {
        assertThatThrownBy(() -> service.getAppointmentInfo(
                PATIENT_ID,
                APPOINTMENT_ID
        )).isInstanceOfSatisfying(
                BaseException.class,
                exception -> assertThat(exception.getBaseResponseCode())
                        .isEqualTo(expected)
        );
    }
}
