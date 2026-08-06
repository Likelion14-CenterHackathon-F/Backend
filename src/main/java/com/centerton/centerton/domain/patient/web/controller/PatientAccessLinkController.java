package com.centerton.centerton.domain.patient.web.controller;

import com.centerton.centerton.domain.patient.service.PatientAccessLinkService;
import com.centerton.centerton.domain.patient.web.dto.PatientAccessLinkCreateReq;
import com.centerton.centerton.domain.patient.web.dto.PatientAccessLinkCreateRes;
import com.centerton.centerton.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/patients")
public class PatientAccessLinkController {

    private final PatientAccessLinkService patientAccessLinkService;

    @PostMapping("/{patientId}/access-links")
    public ResponseEntity<SuccessResponse<PatientAccessLinkCreateRes>> createAccessLink(
            @PathVariable Long patientId,
            @RequestBody(required = false) PatientAccessLinkCreateReq req
    ) {
        PatientAccessLinkCreateRes res = patientAccessLinkService.createAccessLink(patientId, req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SuccessResponse.created(res));
    }
}
