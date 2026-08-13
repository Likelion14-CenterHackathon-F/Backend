package com.centerton.centerton.domain.consultationsummary.service;

import com.centerton.centerton.domain.consultation.entity.ConsultationSession;
import com.centerton.centerton.domain.consultation.entity.TranscriptSegment;
import com.centerton.centerton.domain.consultation.repository.ConsultationSessionRepository;
import com.centerton.centerton.domain.consultation.repository.TranscriptSegmentRepository;
import com.centerton.centerton.domain.consultationsummary.dto.SummaryLanguage;
import com.centerton.centerton.domain.consultationsummary.dto.request.ConsultationSummaryCreateReq;
import com.centerton.centerton.domain.consultationsummary.dto.response.ConsultationSummaryDetailRes;
import com.centerton.centerton.domain.consultationsummary.dto.response.ConsultationSummaryListRes;
import com.centerton.centerton.domain.consultationsummary.dto.response.SummaryInstructionRes;
import com.centerton.centerton.domain.consultationsummary.entity.ConsultationSummary;
import com.centerton.centerton.domain.consultationsummary.entity.SummaryInstruction;
import com.centerton.centerton.domain.consultationsummary.exception.ConsultationSummaryErrorCode;
import com.centerton.centerton.domain.consultationsummary.repository.ConsultationSummaryRepository;
import com.centerton.centerton.domain.consultationsummary.repository.SummaryInstructionRepository;
import com.centerton.centerton.global.exception.BaseException;
import com.centerton.centerton.global.translation.DeepLConfigurationException;
import com.centerton.centerton.global.translation.DeepLTranslationClient;
import com.centerton.centerton.global.translation.DeepLTranslationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
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

    /**
     * 이 메서드에는 @Transactional을 적용하지 않습니다.
     *
     * Gemini와 DeepL 같은 외부 API 호출 중
     * DB 트랜잭션/커넥션을 오래 점유하지 않도록 하기 위함입니다.
     */
    public ConsultationSummaryDetailRes createSummary(
            Long appointmentId,
            ConsultationSummaryCreateReq request,
            SummaryLanguage language
    ) {
        ConsultationSession session =
                sessionRepository.findByAppointmentId(appointmentId)
                        .orElseThrow(() -> new BaseException(
                                ConsultationSummaryErrorCode
                                        .CONSULTATION_SESSION_NOT_FOUND
                        ));

        if (!session.isCompleted()) {
            throw new BaseException(
                    ConsultationSummaryErrorCode.CONSULTATION_NOT_COMPLETED
            );
        }

        ConsultationSummary existingSummary =
                summaryRepository
                        .findByConsultationSessionSessionId(
                                session.getSessionId()
                        )
                        .orElse(null);

        if (existingSummary != null) {
            return toDetailResponse(
                    existingSummary,
                    language
            );
        }

        List<TranscriptSegment> transcripts =
                transcriptRepository
                        .findAllByConsultationSessionSessionIdOrderBySequenceNumberAsc(
                                session.getSessionId()
                        );

        if (transcripts.isEmpty()) {
            throw new BaseException(
                    ConsultationSummaryErrorCode.CONSULTATION_TRANSCRIPT_EMPTY
            );
        }

        /*
         * Gemini 외부 API 호출.
         * DB 트랜잭션이 없는 상태에서 실행됩니다.
         */
        GeminiSummaryService.SummaryResult result =
                geminiSummaryService.summarize(transcripts);

        ConsultationSummary summary =
                ConsultationSummary.create(
                        resolveConsultedAt(session),
                        request.medicalStaffName(),
                        result.summary(),
                        result.patientConsultationDetails(),
                        session
                );

        for (int index = 0;
             index < result.instructions().size();
             index++) {

            summary.addInstruction(
                    result.instructions().get(index),
                    index + 1
            );
        }

        ConsultationSummary savedSummary;

        try {
            /*
             * saveAndFlush를 사용해서 session_id UNIQUE 충돌을
             * 이 시점에서 확실하게 감지합니다.
             */
            savedSummary =
                    summaryRepository.saveAndFlush(summary);

        } catch (DataIntegrityViolationException exception) {

            /*
             * 거의 동시에 동일 상담에 대한 요약 생성 요청이
             * 들어온 경우 다른 요청이 먼저 저장했을 수 있습니다.
             *
             * DB UNIQUE 제약을 최종 방어선으로 사용하고
             * 이미 생성된 요약을 다시 조회하여 반환합니다.
             */
            log.info(
                    "동일 상담의 요약이 이미 생성되었습니다. sessionId={}",
                    session.getSessionId()
            );

            savedSummary =
                    summaryRepository
                            .findByConsultationSessionSessionId(
                                    session.getSessionId()
                            )
                            .orElseThrow(() -> exception);
        }

        /*
         * 저장이 완료된 뒤 DeepL을 호출합니다.
         *
         * 번역 실패가 발생하더라도 이미 저장된 요약이
         * 롤백되지 않습니다.
         */
        return toDetailResponse(
                savedSummary,
                language
        );
    }

    public ConsultationSummaryDetailRes getSummary(
            Long summaryId,
            SummaryLanguage language
    ) {
        ConsultationSummary summary =
                summaryRepository
                        .findBySummaryId(summaryId)
                        .orElseThrow(() -> new BaseException(
                                ConsultationSummaryErrorCode.SUMMARY_NOT_FOUND
                        ));

        return toDetailResponse(
                summary,
                language
        );
    }

    public List<ConsultationSummaryListRes> getSummaries(
            SummaryLanguage language
    ) {
        List<ConsultationSummary> summaries =
                summaryRepository
                        .findAllByOrderByConsultedAtDescSummaryIdDesc();

        if (summaries.isEmpty()) {
            return List.of();
        }

        List<String> koreanSummaries =
                summaries.stream()
                        .map(ConsultationSummary::getTranslatedSummary)
                        .toList();

        /*
         * Repository 조회 트랜잭션이 종료된 뒤
         * DeepL 외부 API가 호출됩니다.
         */
        List<String> translatedSummaries =
                translateKoreanTexts(
                        koreanSummaries,
                        language
                );

        List<ConsultationSummaryListRes> responses =
                new ArrayList<>();

        for (int index = 0;
             index < summaries.size();
             index++) {

            ConsultationSummary summary =
                    summaries.get(index);

            ConsultationSession session =
                    summary.getConsultationSession();

            responses.add(
                    new ConsultationSummaryListRes(
                            summary.getSummaryId(),
                            summary.getConsultedAt(),
                            summary.getMedicalStaffName(),
                            session.getActualDurationSeconds(),
                            language.getResponseCode(),
                            translatedSummaries.get(index),
                            session.getSessionId()
                    )
            );
        }

        return responses;
    }

    @Transactional
    public SummaryInstructionRes changeInstructionCompletion(
            Long summaryId,
            Long instructionId,
            Boolean patientCompleted
    ) {
        SummaryInstruction instruction =
                instructionRepository
                        .findByInstructionIdAndConsultationSummarySummaryId(
                                instructionId,
                                summaryId
                        )
                        .orElseThrow(() -> new BaseException(
                                ConsultationSummaryErrorCode
                                        .SUMMARY_INSTRUCTION_NOT_FOUND
                        ));

        instruction.changeCompletion(
                patientCompleted,
                nowUtc()
        );

        return SummaryInstructionRes.from(instruction);
    }

    private ConsultationSummaryDetailRes toDetailResponse(
            ConsultationSummary summary,
            SummaryLanguage language
    ) {
        List<String> koreanTexts =
                new ArrayList<>();

        koreanTexts.add(
                nonNull(summary.getTranslatedSummary())
        );

        koreanTexts.add(
                nonNull(summary.getConsultationDetails())
        );

        summary.getInstructions()
                .stream()
                .map(SummaryInstruction::getContent)
                .map(this::nonNull)
                .forEach(koreanTexts::add);

        List<String> translatedTexts =
                translateKoreanTexts(
                        koreanTexts,
                        language
                );

        List<SummaryInstructionRes> instructionResponses =
                new ArrayList<>();

        for (int index = 0;
             index < summary.getInstructions().size();
             index++) {

            SummaryInstruction instruction =
                    summary.getInstructions().get(index);

            instructionResponses.add(
                    SummaryInstructionRes.translated(
                            instruction,
                            translatedTexts.get(index + 2)
                    )
            );
        }

        ConsultationSession session =
                summary.getConsultationSession();

        return new ConsultationSummaryDetailRes(
                summary.getSummaryId(),
                summary.getConsultedAt(),
                summary.getMedicalStaffName(),
                session.getActualDurationSeconds(),
                language.getResponseCode(),
                translatedTexts.get(0),
                translatedTexts.get(1),
                instructionResponses,
                session.getSessionId()
        );
    }

    private LocalDateTime resolveConsultedAt(
            ConsultationSession session
    ) {
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

    private List<String> translateKoreanTexts(
            List<String> koreanTexts,
            SummaryLanguage language
    ) {
        try {
            return translationClient.translateKoreanTexts(
                    koreanTexts,
                    language.getDeepLTargetCode()
            );
        } catch (DeepLConfigurationException exception) {
            throw new BaseException(
                    ConsultationSummaryErrorCode.DEEPL_CONFIGURATION_MISSING
            );
        } catch (DeepLTranslationException exception) {
            throw new BaseException(
                    ConsultationSummaryErrorCode.DEEPL_TRANSLATION_FAILED
            );
        }
    }
}
