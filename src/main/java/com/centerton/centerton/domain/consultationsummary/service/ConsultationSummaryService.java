package com.centerton.centerton.domain.consultationsummary.service;

import com.centerton.centerton.domain.consultationsummary.client.DeepLTranslationClient;
import com.centerton.centerton.domain.consultationsummary.dto.SummaryLanguage;
import com.centerton.centerton.domain.consultationsummary.dto.request.ConsultationSummaryCreateReq;
import com.centerton.centerton.domain.consultationsummary.dto.response.ConsultationSummaryDetailRes;
import com.centerton.centerton.domain.consultationsummary.dto.response.ConsultationSummaryListRes;
import com.centerton.centerton.domain.consultationsummary.dto.response.SummaryInstructionRes;
import com.centerton.centerton.domain.consultation.entity.ConsultationSession;
import com.centerton.centerton.domain.consultationsummary.entity.ConsultationSummary;
import com.centerton.centerton.domain.consultationsummary.entity.SummaryInstruction;
import com.centerton.centerton.domain.consultation.entity.TranscriptSegment;
import com.centerton.centerton.domain.consultationsummary.exception.ConsultationSummaryErrorCode;
import com.centerton.centerton.domain.consultation.repository.ConsultationSessionRepository;
import com.centerton.centerton.domain.consultationsummary.repository.ConsultationSummaryRepository;
import com.centerton.centerton.domain.consultationsummary.repository.SummaryInstructionRepository;
import com.centerton.centerton.domain.consultation.repository.TranscriptSegmentRepository;
import com.centerton.centerton.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConsultationSummaryService {

    private final ConsultationSessionRepository sessionRepository;
    private final TranscriptSegmentRepository transcriptRepository;
    private final ConsultationSummaryRepository summaryRepository;
    private final SummaryInstructionRepository instructionRepository;
    private final GeminiSummaryService geminiSummaryService;
    private final DeepLTranslationClient translationClient;

    public ConsultationSummaryService(
            ConsultationSessionRepository sessionRepository,
            TranscriptSegmentRepository transcriptRepository,
            ConsultationSummaryRepository summaryRepository,
            SummaryInstructionRepository instructionRepository,
            GeminiSummaryService geminiSummaryService,
            DeepLTranslationClient translationClient
    ) {
        this.sessionRepository = sessionRepository;
        this.transcriptRepository = transcriptRepository;
        this.summaryRepository = summaryRepository;
        this.instructionRepository = instructionRepository;
        this.geminiSummaryService = geminiSummaryService;
        this.translationClient = translationClient;
    }

    @Transactional
    public ConsultationSummaryDetailRes createSummary(
            Long appointmentId,
            ConsultationSummaryCreateReq request,
            SummaryLanguage language
    ) {
        ConsultationSession session = sessionRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new BaseException(
                        ConsultationSummaryErrorCode.CONSULTATION_SESSION_NOT_FOUND
                ));

        if (!session.isCompleted()) {
            throw new BaseException(
                    ConsultationSummaryErrorCode.CONSULTATION_NOT_COMPLETED
            );
        }

        ConsultationSummary existingSummary = summaryRepository
                .findByConsultationSessionSessionId(session.getSessionId())
                .orElse(null);
        if (existingSummary != null) {
            return toDetailResponse(existingSummary, language);
        }

        List<TranscriptSegment> transcripts = transcriptRepository
                .findAllByConsultationSessionSessionIdOrderBySequenceNumberAsc(
                        session.getSessionId()
                );
        if (transcripts.isEmpty()) {
            throw new BaseException(
                    ConsultationSummaryErrorCode.CONSULTATION_TRANSCRIPT_EMPTY
            );
        }

        GeminiSummaryService.SummaryResult result = geminiSummaryService.summarize(
                transcripts
        );
        ConsultationSummary summary = ConsultationSummary.create(
                resolveConsultedAt(session),
                request.hospitalName(),
                request.medicalStaffName(),
                result.summary(),
                result.patientConsultationDetails(),
                session
        );

        for (int index = 0; index < result.instructions().size(); index++) {
            summary.addInstruction(result.instructions().get(index), index + 1);
        }

        ConsultationSummary savedSummary = summaryRepository.save(summary);
        return toDetailResponse(savedSummary, language);
    }

    public ConsultationSummaryDetailRes getSummary(
            Long summaryId,
            SummaryLanguage language
    ) {
        ConsultationSummary summary = summaryRepository
                .findBySummaryId(summaryId)
                .orElseThrow(() -> new BaseException(
                        ConsultationSummaryErrorCode.SUMMARY_NOT_FOUND
                ));

        return toDetailResponse(summary, language);
    }

    public List<ConsultationSummaryListRes> getSummaries(SummaryLanguage language) {
        List<ConsultationSummary> summaries = summaryRepository
                .findAllByOrderByConsultedAtDescSummaryIdDesc();
        if (summaries.isEmpty()) {
            return List.of();
        }

        List<String> koreanSummaries = summaries.stream()
                .map(ConsultationSummary::getTranslatedSummary)
                .toList();
        List<String> translatedSummaries = translationClient.translateKoreanTexts(
                koreanSummaries,
                language
        );

        List<ConsultationSummaryListRes> responses = new ArrayList<>();
        for (int index = 0; index < summaries.size(); index++) {
            ConsultationSummary summary = summaries.get(index);
            ConsultationSession session = summary.getConsultationSession();
            responses.add(new ConsultationSummaryListRes(
                    summary.getSummaryId(),
                    summary.getConsultedAt(),
                    summary.getHospitalName(),
                    summary.getMedicalStaffName(),
                    session.getActualDurationSeconds(),
                    language.getResponseCode(),
                    translatedSummaries.get(index),
                    session.getSessionId()
            ));
        }
        return responses;
    }

    @Transactional
    public SummaryInstructionRes changeInstructionCompletion(
            Long summaryId,
            Long instructionId,
            Boolean patientCompleted
    ) {
        SummaryInstruction instruction = instructionRepository
                .findByInstructionIdAndConsultationSummarySummaryId(
                        instructionId,
                        summaryId
                )
                .orElseThrow(() -> new BaseException(
                        ConsultationSummaryErrorCode.SUMMARY_INSTRUCTION_NOT_FOUND
                ));

        instruction.changeCompletion(patientCompleted, nowUtc());
        return SummaryInstructionRes.from(instruction);
    }

    private ConsultationSummaryDetailRes toDetailResponse(
            ConsultationSummary summary,
            SummaryLanguage language
    ) {
        List<String> koreanTexts = new ArrayList<>();
        koreanTexts.add(nonNull(summary.getTranslatedSummary()));
        koreanTexts.add(nonNull(summary.getConsultationDetails()));
        summary.getInstructions()
                .stream()
                .map(SummaryInstruction::getContent)
                .map(this::nonNull)
                .forEach(koreanTexts::add);

        List<String> translatedTexts = translationClient.translateKoreanTexts(
                koreanTexts,
                language
        );

        List<SummaryInstructionRes> instructionResponses = new ArrayList<>();
        for (int index = 0; index < summary.getInstructions().size(); index++) {
            SummaryInstruction instruction = summary.getInstructions().get(index);
            instructionResponses.add(SummaryInstructionRes.translated(
                    instruction,
                    translatedTexts.get(index + 2)
            ));
        }

        ConsultationSession session = summary.getConsultationSession();
        return new ConsultationSummaryDetailRes(
                summary.getSummaryId(),
                summary.getConsultedAt(),
                summary.getHospitalName(),
                summary.getMedicalStaffName(),
                session.getActualDurationSeconds(),
                language.getResponseCode(),
                translatedTexts.get(0),
                translatedTexts.get(1),
                instructionResponses,
                session.getSessionId()
        );
    }

    private LocalDateTime resolveConsultedAt(ConsultationSession session) {
        if (session.getStartedAt() != null) {
            return session.getStartedAt();
        }
        return session.getEndedAt();
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private String nonNull(String value) {
        return value == null ? "" : value;
    }
}
