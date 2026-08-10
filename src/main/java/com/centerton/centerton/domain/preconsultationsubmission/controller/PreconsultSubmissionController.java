package com.centerton.centerton.domain.preconsultationsubmission.controller;

import com.centerton.centerton.domain.preconsultationsubmission.dto.request.PreconsultSubmissionCreateReq;
import com.centerton.centerton.domain.preconsultationsubmission.dto.response.PreconsultDownloadFile;
import com.centerton.centerton.domain.preconsultationsubmission.dto.response.PreconsultSubmissionRes;
import com.centerton.centerton.domain.preconsultationsubmission.service.PreconsultSubmissionService;
import com.centerton.centerton.global.jwt.PatientDetails;
import com.centerton.centerton.global.response.SuccessResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/preconsult-submissions")
public class PreconsultSubmissionController {

    private final PreconsultSubmissionService submissionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<PreconsultSubmissionRes> createSubmission(
            @AuthenticationPrincipal PatientDetails patientDetails,
            @Valid @ModelAttribute PreconsultSubmissionCreateReq request
    ) {
        return SuccessResponse.created(
                submissionService.createSubmission(
                        patientDetails.getPatientId(),
                        request
                )
        );
    }

    @GetMapping
    public SuccessResponse<PreconsultSubmissionRes> getSubmission(
            @AuthenticationPrincipal PatientDetails patientDetails,
            @RequestParam @Positive Long appointmentId
    ) {
        return SuccessResponse.from(
                submissionService.getSubmission(
                        patientDetails.getPatientId(),
                        appointmentId
                )
        );
    }

    @GetMapping("/files/{storedFileName:.+}")
    public ResponseEntity<Resource> getFile(
            @AuthenticationPrincipal PatientDetails patientDetails,
            @PathVariable String storedFileName
    ) {
        PreconsultDownloadFile file = submissionService.getFile(
                patientDetails.getPatientId(),
                storedFileName
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(
                                        file.storedFileName(),
                                        StandardCharsets.UTF_8
                                )
                                .build()
                                .toString()
                )
                .header("X-Content-Type-Options", "nosniff")
                .body(file.resource());
    }
}
