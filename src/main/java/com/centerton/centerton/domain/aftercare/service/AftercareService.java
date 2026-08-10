package com.centerton.centerton.domain.aftercare.service;

import com.centerton.centerton.domain.aftercare.dto.response.AftercareDashboardDetailRes;
import com.centerton.centerton.domain.aftercare.dto.response.AftercareHomeRes;
import com.centerton.centerton.domain.aftercare.dto.response.EmergencyMedicalReportRes;
import com.centerton.centerton.domain.aftercare.entity.AftercareCase;
import com.centerton.centerton.domain.aftercare.exception.AftercareErrorCode;
import com.centerton.centerton.domain.aftercare.repository.AftercareCaseRepository;
import com.centerton.centerton.domain.appointment.dto.response.AppointmentHomeSummary;
import com.centerton.centerton.domain.appointment.service.AppointmentQueryService;
import com.centerton.centerton.domain.patient.entity.PatientAllergy;
import com.centerton.centerton.domain.patient.repository.PatientAllergyRepository;
import com.centerton.centerton.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AftercareService {

    private final AftercareCaseRepository aftercareCaseRepository;
    private final PatientAllergyRepository patientAllergyRepository;
    private final AppointmentQueryService appointmentQueryService;

    public AftercareHomeRes getHome(Long patientId) {
        AftercareCase aftercareCase = getHomeAftercareCase(patientId);
        validateProcedureRecord(aftercareCase);

        AppointmentHomeSummary appointment = appointmentQueryService.findHomeAppointment(
                patientId,
                aftercareCase.getCaseId()
        ).orElse(null);

        // TODO: 환자 국적/언어 기준으로 홈 화면 응답을 번역해서 반환.
        return AftercareHomeRes.from(aftercareCase, resolveToday(), appointment);
    }

    public AftercareDashboardDetailRes getDashboardDetail(Long patientId) {
        AftercareCase aftercareCase = getDashboardAftercareCase(patientId);
        validateProcedureRecord(aftercareCase);

        // TODO: 환자 국적/언어 기준으로 사후관리 상세 응답을 번역해서 반환.
        return AftercareDashboardDetailRes.from(aftercareCase, resolveToday());
    }

    public EmergencyMedicalReportRes getEmergencyMedicalReport(Long patientId) {
        AftercareCase aftercareCase = getEmergencyReportAftercareCase(patientId);
        validateProcedureRecord(aftercareCase);

        List<PatientAllergy> allergies = patientAllergyRepository.findAllByPatientIdOrderByAllergyIdAsc(patientId);

        return EmergencyMedicalReportRes.from(aftercareCase, allergies);
    }

    private AftercareCase getHomeAftercareCase(Long patientId) {
        return aftercareCaseRepository.findHomeByPatientId(patientId)
                .orElseThrow(() -> new BaseException(AftercareErrorCode.AFTERCARE_CASE_NOT_FOUND));
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

    private LocalDate resolveToday() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}
