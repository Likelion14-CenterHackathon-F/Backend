package com.centerton.centerton.domain.patient.web.controller;

import com.centerton.centerton.domain.patient.service.PatientService;
import com.centerton.centerton.domain.patient.web.dto.PatientSettingsUpdateReq;
import com.centerton.centerton.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;

    @PatchMapping("/{patientId}/settings")
    public ResponseEntity<SuccessResponse<Void>> updateSettings(
            @PathVariable Long patientId,
            @Valid @RequestBody PatientSettingsUpdateReq request
    ) {
        patientService.updateSettings(patientId, request);

        return ResponseEntity.status(HttpStatus.OK).body(SuccessResponse.empty());
    }
}
