package com.centerton.centerton.domain.aftercare.service;

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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AftercareService {

    private final AftercareCaseRepository aftercareCaseRepository;
    private final PatientAllergyRepository patientAllergyRepository;

    public EmergencyMedicalReportRes getEmergencyMedicalReport(Long patientId) {
        AftercareCase aftercareCase =
                aftercareCaseRepository.findByPatientId(patientId)
                        .orElseThrow(() -> new BaseException(AftercareErrorCode.AFTERCARE_CASE_NOT_FOUND));

        if (aftercareCase.getProcedureRecord() == null) {
            throw new BaseException(AftercareErrorCode.PROCEDURE_RECORD_NOT_FOUND);
        }

        List<PatientAllergy> allergies = patientAllergyRepository.findAllByPatientIdOrderByAllergyIdAsc(patientId);

        return EmergencyMedicalReportRes.from(aftercareCase, allergies);
    }
}
