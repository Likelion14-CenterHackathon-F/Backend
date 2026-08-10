package com.centerton.centerton.domain.aftercare.service;

import com.centerton.centerton.domain.aftercare.dto.response.AftercareDashboardDetailRes;
import com.centerton.centerton.domain.aftercare.dto.response.EmergencyMedicalReportRes;
import com.centerton.centerton.domain.aftercare.entity.AftercareCase;
import com.centerton.centerton.domain.aftercare.exception.AftercareErrorCode;
import com.centerton.centerton.domain.aftercare.repository.AftercareCaseRepository;
import com.centerton.centerton.domain.patient.entity.PatientAllergy;
import com.centerton.centerton.domain.patient.repository.PatientAllergyRepository;
import com.centerton.centerton.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AftercareService {

    private final AftercareCaseRepository aftercareCaseRepository;
    private final PatientAllergyRepository patientAllergyRepository;

    public AftercareDashboardDetailRes getDashboardDetail(Long patientId) {
        AftercareCase aftercareCase = getDashboardAftercareCase(patientId);
        validateProcedureRecord(aftercareCase);

        return AftercareDashboardDetailRes.from(aftercareCase, resolveToday(aftercareCase));
    }

    public EmergencyMedicalReportRes getEmergencyMedicalReport(Long patientId) {
        AftercareCase aftercareCase = getEmergencyReportAftercareCase(patientId);
        validateProcedureRecord(aftercareCase);

        List<PatientAllergy> allergies = patientAllergyRepository.findAllByPatientIdOrderByAllergyIdAsc(patientId);

        return EmergencyMedicalReportRes.from(aftercareCase, allergies);
    }

    private AftercareCase getDashboardAftercareCase(Long patientId) {
        return aftercareCaseRepository.findDashboardByPatientId(patientId)
                .orElseThrow(() -> new BaseException(AftercareErrorCode.AFTERCARE_CASE_NOT_FOUND));
    }

    private AftercareCase getEmergencyReportAftercareCase(Long patientId) {
        return aftercareCaseRepository.findEmergencyReportByPatientId(patientId)
                .orElseThrow(() -> new BaseException(AftercareErrorCode.AFTERCARE_CASE_NOT_FOUND));
    }

    private void validateProcedureRecord(AftercareCase aftercareCase) {
        if (aftercareCase.getProcedureRecord() == null) {
            throw new BaseException(AftercareErrorCode.PROCEDURE_RECORD_NOT_FOUND);
        }
    }

    private LocalDate resolveToday(AftercareCase aftercareCase) {
        try {
            String timezoneId = aftercareCase.getPatient().getTimezoneId();
            return LocalDate.now(timezoneId == null ? ZoneId.of("Asia/Seoul") : ZoneId.of(timezoneId));
        } catch (DateTimeException exception) {
            return LocalDate.now(ZoneId.of("Asia/Seoul"));
        }
    }
}
