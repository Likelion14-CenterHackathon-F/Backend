package com.centerton.centerton.domain.aftercare.service;

import com.centerton.centerton.domain.aftercare.dto.response.AftercareDashboardDetailRes;
import com.centerton.centerton.domain.aftercare.dto.response.AftercareHomeRes;
import com.centerton.centerton.domain.aftercare.dto.response.EmergencyMedicalReportRes;
import com.centerton.centerton.domain.patient.entity.enums.Language;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AftercareService {

    private final AftercareQueryService queryService;
    private final AftercareResponseTranslator responseTranslator;

    public AftercareHomeRes getHome(
            Long patientId,
            Language language
    ) {
        return responseTranslator.translateHome(
                queryService.getHome(patientId),
                language
        );
    }

    public AftercareDashboardDetailRes getDashboardDetail(
            Long patientId,
            Language language
    ) {
        return responseTranslator.translateDashboardDetail(
                queryService.getDashboardDetail(patientId),
                language
        );
    }

    public EmergencyMedicalReportRes getEmergencyMedicalReport(Long patientId) {
        return queryService.getEmergencyMedicalReport(patientId);
    }
}
