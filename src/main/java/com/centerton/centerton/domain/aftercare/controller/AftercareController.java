package com.centerton.centerton.domain.aftercare.controller;

import com.centerton.centerton.domain.aftercare.dto.response.AftercareDashboardDetailRes;
import com.centerton.centerton.domain.aftercare.dto.response.EmergencyMedicalReportRes;
import com.centerton.centerton.domain.aftercare.service.AftercareService;
import com.centerton.centerton.global.jwt.PatientDetails;
import com.centerton.centerton.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/aftercare")
public class AftercareController {

    private final AftercareService aftercareService;

    @GetMapping("/dashboard")
    public SuccessResponse<AftercareDashboardDetailRes> getDashboardDetail(@AuthenticationPrincipal PatientDetails patientDetails) {
        return SuccessResponse.from(aftercareService.getDashboardDetail(patientDetails.getPatientId()));
    }

    @GetMapping("/emergency-medical-report")
    public SuccessResponse<EmergencyMedicalReportRes> getEmergencyMedicalReport(@AuthenticationPrincipal PatientDetails patientDetails) {
        return SuccessResponse.from(aftercareService.getEmergencyMedicalReport(patientDetails.getPatientId()));
    }
}
