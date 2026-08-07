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
import com.centerton.centerton.global.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/consultations")
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
    public SuccessResponse<JoinConsultationRes> join(
            @PathVariable Long appointmentId,
            @Valid @RequestBody JoinConsultationReq request
    ) {
        return SuccessResponse.from(consultationService.join(appointmentId, request));
    }

    @PostMapping("/{appointmentId}/token/renew")
    public SuccessResponse<TokenRes> renewToken(
            @PathVariable Long appointmentId,
            @Valid @RequestBody TokenRenewReq request
    ) {
        return SuccessResponse.from(consultationService.renewToken(appointmentId, request));
    }

    @PostMapping("/{appointmentId}/transcription/start")
    public SuccessResponse<TranscriptionRes> startTranscription(
            @PathVariable Long appointmentId
    ) {
        return SuccessResponse.from(consultationService.startTranscription(appointmentId));
    }

    @GetMapping("/{appointmentId}/transcription/status")
    public SuccessResponse<TranscriptionRes> getTranscriptionStatus(
            @PathVariable Long appointmentId
    ) {
        return SuccessResponse.from(consultationService.getTranscriptionStatus(appointmentId));
    }

    @PostMapping("/{appointmentId}/captions/batch")
    public SuccessResponse<CaptionBatchRes> saveCaptionBatch(
            @PathVariable Long appointmentId,
            @Valid @RequestBody CaptionBatchReq request
    ) {
        return SuccessResponse.from(captionService.saveBatch(appointmentId, request));
    }

    @GetMapping("/{appointmentId}/captions")
    public SuccessResponse<List<CaptionRes>> getCaptions(
            @PathVariable Long appointmentId
    ) {
        return SuccessResponse.from(captionService.getCaptions(appointmentId));
    }

    @PostMapping("/{appointmentId}/end")
    public SuccessResponse<ConsultationEndRes> end(
            @PathVariable Long appointmentId
    ) {
        return SuccessResponse.from(consultationService.end(appointmentId));
    }

    @GetMapping("/history")
    public SuccessResponse<List<ConsultationHistoryRes>> getHistory() {
        return SuccessResponse.from(consultationService.getHistory());
    }
}
