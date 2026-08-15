package com.centerton.centerton.domain.preconsultationsubmission.service;

import com.centerton.centerton.domain.appointment.repository.AppointmentRepository;
import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;
import com.centerton.centerton.domain.preconsultationsubmission.exception.PreconsultSubmissionErrorCode;
import com.centerton.centerton.domain.preconsultationsubmission.repository.FileAssetRepository;
import com.centerton.centerton.domain.preconsultationsubmission.repository.PreconsultSubmissionRepository;
import com.centerton.centerton.domain.preconsultationsubmission.storage.PreconsultFileStorage;
import com.centerton.centerton.domain.preconsultationsubmission.storage.PreconsultFileValidator;
import com.centerton.centerton.global.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PreconsultSubmissionServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private PreconsultSubmissionRepository submissionRepository;
    @Mock
    private FileAssetRepository fileAssetRepository;
    @Mock
    private PreconsultFileStorage fileStorage;
    @Mock
    private PreconsultFileValidator fileValidator;

    @InjectMocks
    private PreconsultSubmissionService service;

    @Test
    void noteOnlySubmissionIsAllowed() {
        PreconsultSubmissionService.PreparedPreconsultSubmission prepared =
                service.prepareSubmission(
                        Set.of(),
                        "  저녁에 붓기가 심해집니다.  ",
                        List.of()
                );

        assertThat(prepared.symptomCategories()).isEmpty();
        assertThat(prepared.symptomNote())
                .isEqualTo("저녁에 붓기가 심해집니다.");
    }

    @Test
    void submissionWithoutSymptomsNoteOrFilesIsRejected() {
        assertThatThrownBy(() -> service.prepareSubmission(
                Set.of(),
                null,
                List.of()
        )).isInstanceOfSatisfying(
                BaseException.class,
                exception -> assertThat(exception.getBaseResponseCode())
                        .isEqualTo(
                                PreconsultSubmissionErrorCode.SUBMISSION_CONTENT_REQUIRED
                        )
        );
    }

    @Test
    void preparedSymptomsAreDeduplicatedAndDefensivelyCopied() {
        Set<SymptomCategory> requested = new HashSet<>();
        requested.add(SymptomCategory.SWELLING);
        requested.add(null);

        PreconsultSubmissionService.PreparedPreconsultSubmission prepared =
                service.prepareSubmission(requested, null, List.of());
        requested.add(SymptomCategory.BRUISING);

        assertThat(prepared.symptomCategories())
                .containsExactly(SymptomCategory.SWELLING);
        assertThatThrownBy(() -> prepared.symptomCategories()
                .add(SymptomCategory.BRUISING))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
