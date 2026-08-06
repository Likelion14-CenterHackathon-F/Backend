package com.centerton.centerton.domain.consultationsummary.controller;

import com.centerton.centerton.domain.consultation.dto.request.JoinConsultationReq;
import com.centerton.centerton.domain.consultation.dto.response.JoinConsultationRes;
import com.centerton.centerton.domain.consultation.service.CaptionService;
import com.centerton.centerton.domain.consultation.service.ConsultationService;
import com.centerton.centerton.domain.consultationsummary.dto.SummaryLanguage;
import com.centerton.centerton.domain.consultationsummary.dto.request.ConsultationSummaryCreateReq;
import com.centerton.centerton.domain.consultationsummary.dto.response.ConsultationSummaryDetailRes;
import com.centerton.centerton.domain.consultationsummary.exception.ConsultationSummaryErrorCode;
import com.centerton.centerton.domain.consultationsummary.service.ConsultationSummaryService;
import com.centerton.centerton.global.exception.BaseException;
import com.centerton.centerton.global.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consultation-summaries")
public class ConsultationSummaryController {

    private final ConsultationSummaryService consultationSummaryService;

    public ConsultationSummaryController(
            ConsultationSummaryService consultationSummaryService
    ) {
        this.consultationSummaryService = consultationSummaryService;
    }

    @PostMapping("/{appointmentId}")
    public SuccessResponse<ConsultationSummaryDetailRes> createSummary(
            @PathVariable Long appointmentId,
            @Valid @RequestBody ConsultationSummaryCreateReq request
    ) {
        return SuccessResponse.from(consultationSummaryService.createSummary(
                appointmentId,
                request,
                parseLanguage(request.language())
        ));
    }

    @GetMapping("/{summaryId}")
    public SuccessResponse<ConsultationSummaryDetailRes> getSummary(
            @PathVariable Long summaryId,
            @RequestParam(defaultValue = "KO") String language
    ) {
        return SuccessResponse.from(consultationSummaryService.getSummary(
                summaryId,
                parseLanguage(language)
        ));
    }

    private SummaryLanguage parseLanguage(String value) {
        try {
            return SummaryLanguage.from(value);
        } catch (IllegalArgumentException exception) {
            throw new BaseException(
                    ConsultationSummaryErrorCode.UNSUPPORTED_SUMMARY_LANGUAGE
            );
        }
    }
}
