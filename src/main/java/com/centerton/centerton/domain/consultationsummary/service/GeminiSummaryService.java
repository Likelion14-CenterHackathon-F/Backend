package com.centerton.centerton.domain.consultationsummary.service;

import com.centerton.centerton.domain.consultationsummary.client.GeminiClient;
import com.centerton.centerton.domain.consultation.entity.TranscriptSegment;
import com.centerton.centerton.domain.consultationsummary.exception.ConsultationSummaryErrorCode;
import com.centerton.centerton.global.exception.BaseException;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashSet;
import java.util.List;

@Service
public class GeminiSummaryService {

    private static final int SUMMARY_MAX_LENGTH = 300;
    private static final int PATIENT_DETAILS_MAX_LENGTH = 2000;
    private static final int INSTRUCTION_MAX_LENGTH = 300;
    private static final int INSTRUCTION_MAX_COUNT = 10;

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public GeminiSummaryService(
            GeminiClient geminiClient,
            ObjectMapper objectMapper
    ) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    public SummaryResult summarize(List<TranscriptSegment> transcriptSegments) {
        String prompt = createPrompt(transcriptSegments);
        String jsonResponse = geminiClient.generate(prompt);

        try {
            GeminiSummaryPayload payload = objectMapper.readValue(
                    jsonResponse,
                    GeminiSummaryPayload.class
            );

            return normalize(payload);
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new BaseException(
                    ConsultationSummaryErrorCode.GEMINI_SUMMARY_FAILED
            );
        }
    }

    private String createPrompt(List<TranscriptSegment> transcriptSegments) {
        StringBuilder transcript = new StringBuilder();
        for (TranscriptSegment segment : transcriptSegments) {
            transcript.append('[')
                    .append(segment.getSpeakerRole().getValue())
                    .append("] ")
                    .append(resolveReadableText(segment))
                    .append('\n');
        }

        return """
                당신은 피부과·성형외과 시술 후 화상상담 기록 정리 도우미입니다.
                아래 기록에 실제로 등장한 내용만 사용하고 새로운 진단, 처방, 약물 지시를 만들지 마세요.

                작성 규칙:
                1. summary는 한국어로 작성하고 공백 포함 300자를 넘지 않습니다.
                2. 환자의 현재 상태, 의료진의 설명, 향후 관찰 포인트를 간결하게 정리합니다.
                3. patientConsultationDetails는 환자가 직접 호소하거나 문의한 핵심 내용만 한국어로 정리합니다.
                4. instructions에는 의료진이 실제로 말한 지시사항과 후속조치만 행동 문장으로 담습니다.
                5. 불명확하거나 기록에 없는 내용은 추측하지 않습니다.
                6. 인사, 반복 발화, 자막 오류로 보이는 단편은 제외합니다.

                상담 기록:
                """ + transcript;
    }

    private String resolveReadableText(TranscriptSegment segment) {
        if (isKorean(segment.getSourceLanguage())) {
            return segment.getSourceText();
        }

        if (isKorean(segment.getTargetLanguage())
                && segment.getTranslatedText() != null
                && !segment.getTranslatedText().isBlank()) {
            return segment.getTranslatedText();
        }
        return segment.getSourceText();
    }

    private SummaryResult normalize(GeminiSummaryPayload payload) {
        if (payload == null || payload.summary() == null || payload.summary().isBlank()) {
            throw new IllegalArgumentException("Gemini 요약이 비어 있습니다.");
        }

        String summary = limit(payload.summary().trim(), SUMMARY_MAX_LENGTH);
        String patientDetails = payload.patientConsultationDetails() == null
                ? ""
                : limit(
                payload.patientConsultationDetails().trim(),
                PATIENT_DETAILS_MAX_LENGTH
        );

        LinkedHashSet<String> uniqueInstructions = new LinkedHashSet<>();
        if (payload.instructions() != null) {
            payload.instructions().stream()
                    .filter(instruction -> instruction != null && !instruction.isBlank())
                    .map(String::trim)
                    .map(instruction -> limit(instruction, INSTRUCTION_MAX_LENGTH))
                    .limit(INSTRUCTION_MAX_COUNT)
                    .forEach(uniqueInstructions::add);
        }

        return new SummaryResult(
                summary,
                patientDetails,
                List.copyOf(uniqueInstructions)
        );
    }

    private String limit(String value, int maxLength) {
        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= maxLength) {
            return value;
        }

        int endIndex = value.offsetByCodePoints(0, maxLength);
        return value.substring(0, endIndex).trim();
    }

    private boolean isKorean(String language) {
        if (language == null) {
            return false;
        }

        String normalized = language.trim().toUpperCase();
        return normalized.startsWith("KO")
                || normalized.equals("KOREAN")
                || normalized.equals("한국어");
    }

    private record GeminiSummaryPayload(
            String summary,
            String patientConsultationDetails,
            List<String> instructions
    ) {
    }

    public record SummaryResult(
            String summary,
            String patientConsultationDetails,
            List<String> instructions
    ) {
    }
}
