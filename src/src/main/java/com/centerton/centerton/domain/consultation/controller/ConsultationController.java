package com.centerton.centerton.domain.consultation.controller;

import com.centerton.centerton.domain.consultation.dto.request.CaptionBatchReq;
import com.centerton.centerton.domain.consultation.dto.request.JoinConsultationReq;
import com.centerton.centerton.domain.consultation.dto.request.TokenRenewReq;
import com.centerton.centerton.domain.consultation.dto.response.CaptionBatchRes;
import com.centerton.centerton.domain.consultation.dto.response.CaptionRes;
import com.centerton.centerton.domain.consultation.dto.response.ConsultationEndRes;
import com.centerton.centerton.domain.consultation.dto.response.ConsultationHistoryRes;
import com.centerton.centerton.domain.consultation.dto.response.JoinConsultationRes;
import com.centerton.centerton.domain.consultation.dto.response.TokenRes;
import com.centerton.centerton.domain.consultation.dto.response.TranscriptionRes;
import com.centerton.centerton.domain.consultation.service.CaptionService;
import com.centerton.centerton.domain.consultation.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;
    private final CaptionService captionService;

    public ConsultationController(
            ConsultationService consultationService,
            CaptionService captionService
    ) {
        this.consultationService = consultationService;
        this.captionService = captionService;
    }

    @PostMapping("/{appointmentId}/join")
    public ResponseEntity<JoinConsultationRes> join(
            @PathVariable Long appointmentId,
            @Valid @RequestBody JoinConsultationReq request
    ) {
        return ResponseEntity.ok(
                consultationService.join(appointmentId, request)
        );
    }

    @PostMapping("/{appointmentId}/token/renew")
    public ResponseEntity<TokenRes> renewToken(
            @PathVariable Long appointmentId,
            @Valid @RequestBody TokenRenewReq request
    ) {
        return ResponseEntity.ok(
                consultationService.renewToken(appointmentId, request)
        );
    }

    @PostMapping("/{appointmentId}/transcription/start")
    public ResponseEntity<TranscriptionRes> startTranscription(
            @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(
                consultationService.startTranscription(appointmentId)
        );
    }

    @GetMapping("/{appointmentId}/transcription/status")
    public ResponseEntity<TranscriptionRes> getTranscriptionStatus(
            @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(
                consultationService.getTranscriptionStatus(appointmentId)
        );
    }

    @PostMapping("/{appointmentId}/captions/batch")
    public ResponseEntity<CaptionBatchRes> saveCaptionBatch(
            @PathVariable Long appointmentId,
            @Valid @RequestBody CaptionBatchReq request
    ) {
        return ResponseEntity.ok(
                captionService.saveBatch(appointmentId, request)
        );
    }

    @GetMapping("/{appointmentId}/captions")
    public ResponseEntity<List<CaptionRes>> getCaptions(
            @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(
                captionService.getCaptions(appointmentId)
        );
    }

    @PostMapping("/{appointmentId}/end")
    public ResponseEntity<ConsultationEndRes> end(
            @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(
                consultationService.end(appointmentId)
        );
    }

    @GetMapping("/history")
    public ResponseEntity<List<ConsultationHistoryRes>> getHistory() {
        return ResponseEntity.ok(
                consultationService.getHistory()
        );
    }
}
