package com.centerton.centerton.domain.appointment.service;

import com.centerton.centerton.domain.aftercare.entity.AftercareCase;
import com.centerton.centerton.domain.aftercare.repository.AftercareCaseRepository;
import com.centerton.centerton.domain.appointment.dto.request.AppointmentCreateReq;
import com.centerton.centerton.domain.appointment.entity.Appointment;
import com.centerton.centerton.domain.appointment.entity.ReservationSlot;
import com.centerton.centerton.domain.appointment.entity.enums.AppointmentCancelReason;
import com.centerton.centerton.domain.appointment.entity.enums.AppointmentStatus;
import com.centerton.centerton.domain.appointment.repository.AppointmentRepository;
import com.centerton.centerton.domain.appointment.repository.ReservationSlotRepository;
import com.centerton.centerton.domain.patient.entity.Patient;
import com.centerton.centerton.domain.patient.repository.PatientRepository;
import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;
import com.centerton.centerton.domain.preconsultationsubmission.repository.PreconsultSubmissionRepository;
import com.centerton.centerton.domain.preconsultationsubmission.service.PreconsultSubmissionService;
import com.centerton.centerton.global.exception.BaseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(AppointmentReservationTransactionService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AppointmentReservationTransactionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(
            2026, 8, 14, 12, 0
    );

    @Autowired
    private AppointmentReservationTransactionService service;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ReservationSlotRepository slotRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AftercareCaseRepository aftercareCaseRepository;

    @Autowired
    private PreconsultSubmissionRepository submissionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @BeforeEach
    void cleanDatabase() {
        submissionRepository.deleteAll();
        appointmentRepository.deleteAll();
        slotRepository.deleteAll();
        aftercareCaseRepository.deleteAll();
        patientRepository.deleteAll();
    }

    @AfterEach
    void shutdownExecutor() {
        executor.shutdownNow();
    }

    @Test
    void onlyOneConcurrentReservationSucceedsForTheSameSlot() throws Exception {
        Patient firstPatient = savePatient("first@example.com");
        Patient secondPatient = savePatient("second@example.com");
        Long firstCaseId = saveAftercareCase(firstPatient);
        Long secondCaseId = saveAftercareCase(secondPatient);
        ReservationSlot slot = slotRepository.saveAndFlush(
                ReservationSlot.create(
                        NOW.plusHours(2),
                        NOW.plusHours(2).plusMinutes(30)
                )
        );
        CountDownLatch start = new CountDownLatch(1);

        Future<Boolean> first = executor.submit(() -> reserve(
                start,
                firstPatient.getId(),
                firstCaseId,
                slot.getSlotId()
        ));
        Future<Boolean> second = executor.submit(() -> reserve(
                start,
                secondPatient.getId(),
                secondCaseId,
                slot.getSlotId()
        ));

        start.countDown();

        assertThat(List.of(first.get(), second.get()))
                .containsExactlyInAnyOrder(true, false);
        assertThat(appointmentRepository.count()).isEqualTo(1);
        assertThat(submissionRepository.count()).isEqualTo(1);
        Appointment savedAppointment = appointmentRepository.findAll().getFirst();
        assertThat(submissionRepository
                .findByAppointmentAppointmentId(
                        savedAppointment.getAppointmentId()
                ))
                .isPresent();
    }

    @Test
    void cancelledSlotCanBeReservedAgain() {
        Patient firstPatient = savePatient("first@example.com");
        Patient secondPatient = savePatient("second@example.com");
        Long firstCaseId = saveAftercareCase(firstPatient);
        Long secondCaseId = saveAftercareCase(secondPatient);
        ReservationSlot slot = slotRepository.saveAndFlush(
                ReservationSlot.create(
                        NOW.plusHours(2),
                        NOW.plusHours(2).plusMinutes(30)
                )
        );

        AppointmentReservationTransactionService.CreatedAppointment first =
                service.createAppointment(
                        firstPatient.getId(),
                        request(firstCaseId, slot.getSlotId()),
                        preparedSubmission(),
                        NOW
                );

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Appointment appointment = appointmentRepository
                    .findByIdForUpdate(
                            first.appointment().getAppointmentId()
                    )
                    .orElseThrow();
            ReservationSlot lockedSlot = slotRepository
                    .findByIdForUpdate(slot.getSlotId())
                    .orElseThrow();
            appointment.cancel(AppointmentCancelReason.OTHER, NOW);
            lockedSlot.release();
        });

        AppointmentReservationTransactionService.CreatedAppointment second =
                service.createAppointment(
                        secondPatient.getId(),
                        request(secondCaseId, slot.getSlotId()),
                        preparedSubmission(),
                        NOW
                );

        assertThat(second.appointment().getStatus())
                .isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(appointmentRepository.count()).isEqualTo(2);
        assertThat(appointmentRepository.findAll())
                .extracting(Appointment::getStatus)
                .containsExactlyInAnyOrder(
                        AppointmentStatus.CANCELLED,
                        AppointmentStatus.CONFIRMED
                );
        Appointment cancelled = appointmentRepository.findAll().stream()
                .filter(Appointment::isCancelled)
                .findFirst()
                .orElseThrow();
        assertThat(cancelled.getCancelReason())
                .isEqualTo(AppointmentCancelReason.OTHER);
        assertThat(cancelled.getCancelledAt()).isEqualTo(NOW);
        assertThat(appointmentRepository.existsBySlotIdAndStatus(
                slot.getSlotId(),
                AppointmentStatus.CONFIRMED
        )).isTrue();
    }

    @Test
    void multipleSymptomCategoriesAreStoredWithoutDuplicates() {
        Patient patient = savePatient("multiple@example.com");
        Long caseId = saveAftercareCase(patient);
        ReservationSlot slot = slotRepository.saveAndFlush(
                ReservationSlot.create(
                        NOW.plusHours(2),
                        NOW.plusHours(2).plusMinutes(30)
                )
        );

        AppointmentReservationTransactionService.CreatedAppointment created =
                service.createAppointment(
                        patient.getId(),
                        request(caseId, slot.getSlotId()),
                        new PreconsultSubmissionService.PreparedPreconsultSubmission(
                                Set.of(
                                        SymptomCategory.BRUISING,
                                        SymptomCategory.SWELLING
                                ),
                                "붓기와 멍",
                                List.of()
                        ),
                        NOW
                );

        assertThat(submissionRepository.findByAppointmentAppointmentId(
                created.appointment().getAppointmentId()
        ).orElseThrow().getOrderedSymptomCategories()).containsExactly(
                SymptomCategory.SWELLING,
                SymptomCategory.BRUISING
        );
    }

    private boolean reserve(
            CountDownLatch start,
            Long patientId,
            Long caseId,
            Long slotId
    ) throws InterruptedException {
        start.await();
        try {
            service.createAppointment(
                    patientId,
                    request(caseId, slotId),
                    preparedSubmission(),
                    NOW
            );
            return true;
        } catch (BaseException exception) {
            return false;
        }
    }

    private Patient savePatient(String email) {
        return patientRepository.saveAndFlush(Patient.builder()
                .name(email)
                .email(email)
                .birthDate(LocalDate.of(2000, 1, 1))
                .build());
    }

    private Long saveAftercareCase(Patient patient) {
        return aftercareCaseRepository.saveAndFlush(
                AftercareCase.create(patient, NOW.toLocalDate(), 14)
        ).getCaseId();
    }

    private AppointmentCreateReq request(Long caseId, Long slotId) {
        AppointmentCreateReq request = new AppointmentCreateReq();
        request.setCaseId(caseId);
        request.setSlotId(slotId);
        request.setSymptomCategory(SymptomCategory.OTHER);
        return request;
    }

    private PreconsultSubmissionService.PreparedPreconsultSubmission
    preparedSubmission() {
        return new PreconsultSubmissionService.PreparedPreconsultSubmission(
                Set.of(SymptomCategory.OTHER),
                null,
                List.of()
        );
    }
}
