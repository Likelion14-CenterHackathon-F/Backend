package com.centerton.centerton.domain.consultationsummary.service;

import com.centerton.centerton.domain.consultation.entity.TranscriptSegment;
import com.centerton.centerton.domain.consultationsummary.client.GeminiClient;
import com.centerton.centerton.domain.consultationsummary.exception.ConsultationSummaryErrorCode;
import com.centerton.centerton.global.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Service
public class GeminiSummaryService {

    private static final int SUMMARY_MAX_LENGTH = 300;
    private static final int PATIENT_DETAILS_MAX_LENGTH = 2000;
    private static final int INSTRUCTION_MAX_LENGTH = 300;
    private static final int INSTRUCTION_MAX_COUNT = 10;

    /**
     * 최초 요청이 잘못된 JSON을 반환한 경우 한 번만 재요청합니다.
     */
    private static final int MAX_GENERATION_ATTEMPTS = 2;

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public GeminiSummaryService(
            GeminiClient geminiClient,
            ObjectMapper objectMapper
    ) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    public SummaryResult summarize(
            List<TranscriptSegment> transcriptSegments
    ) {
        String originalPrompt = createPrompt(transcriptSegments);
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            String requestPrompt = attempt == 1
                    ? originalPrompt
                    : createRetryPrompt(originalPrompt);

            String rawResponse = geminiClient.generate(requestPrompt);

            try {
                GeminiSummaryPayload payload = parsePayload(rawResponse);
                return normalize(payload);

            } catch (JacksonException | IllegalArgumentException exception) {
                lastException = exception;

                log.warn(
                        "Gemini 상담 요약 응답 파싱 실패. attempt={}, responseLength={}, exceptionType={}, message={}",
                        attempt,
                        rawResponse == null ? 0 : rawResponse.length(),
                        exception.getClass().getSimpleName(),
                        exception.getMessage()
                );
            }
        }

        log.error(
                "Gemini 상담 요약 응답을 JSON으로 변환하지 못했습니다.",
                lastException
        );

        throw new BaseException(
                ConsultationSummaryErrorCode.GEMINI_SUMMARY_FAILED
        );
    }

    /**
     * Gemini 응답에서 완전한 JSON 객체만 추출한 뒤 DTO로 변환합니다.
     *
     * 처리 가능한 응답 예시:
     *
     * ```json
     * {
     *   "summary": "...",
     *   "patientConsultationDetails": "...",
     *   "instructions": []
     * }
     * ```
     *
     * 또는:
     *
     * 요청하신 결과입니다.
     * {
     *   "summary": "...",
     *   "patientConsultationDetails": "...",
     *   "instructions": []
     * }
     */
    private GeminiSummaryPayload parsePayload(
            String rawResponse
    ) throws JacksonException {
        String json = extractCompleteJsonObject(rawResponse);

        return objectMapper.readValue(
                json,
                GeminiSummaryPayload.class
        );
    }

    /**
     * 문자열에서 처음 등장하는 완전한 JSON 객체를 추출합니다.
     *
     * 단순히 첫 번째 '{'와 마지막 '}'를 자르는 방식이 아니라,
     * 문자열 내부의 중괄호와 이스케이프 문자까지 고려합니다.
     */
    private String extractCompleteJsonObject(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException(
                    "Gemini 응답이 비어 있습니다."
            );
        }

        String response = rawResponse.trim();

        // UTF-8 BOM 제거
        if (!response.isEmpty() && response.charAt(0) == '\uFEFF') {
            response = response.substring(1).trim();
        }

        int startIndex = response.indexOf('{');

        if (startIndex < 0) {
            throw new IllegalArgumentException(
                    "Gemini 응답에서 JSON 시작 문자를 찾을 수 없습니다."
            );
        }

        int depth = 0;
        boolean insideString = false;
        boolean escaped = false;

        for (int index = startIndex; index < response.length(); index++) {
            char current = response.charAt(index);

            if (insideString) {
                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (current == '\\') {
                    escaped = true;
                    continue;
                }

                if (current == '"') {
                    insideString = false;
                }

                continue;
            }

            if (current == '"') {
                insideString = true;
                continue;
            }

            if (current == '{') {
                depth++;
                continue;
            }

            if (current == '}') {
                depth--;

                if (depth == 0) {
                    return response.substring(
                            startIndex,
                            index + 1
                    );
                }

                if (depth < 0) {
                    throw new IllegalArgumentException(
                            "Gemini JSON의 중괄호 구조가 올바르지 않습니다."
                    );
                }
            }
        }

        /*
         * 시작 중괄호는 있지만 닫는 중괄호가 없다는 뜻입니다.
         * 대부분 maxOutputTokens 때문에 응답이 중간에 잘린 경우입니다.
         */
        throw new IllegalArgumentException(
                "Gemini JSON 응답이 중간에 잘렸거나 닫히지 않았습니다."
        );
    }

    private String createPrompt(
            List<TranscriptSegment> transcriptSegments
    ) {
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
                아래 상담 기록에 실제로 등장한 내용만 사용하세요.
                새로운 진단, 처방, 약물 지시를 만들지 마세요.

                작성 규칙:
                1. summary는 한국어로 작성하고 공백 포함 300자를 넘지 않습니다.
                2. summary에는 환자의 현재 상태, 의료진의 설명, 향후 관찰 포인트를 간결하게 정리합니다.
                3. patientConsultationDetails에는 환자가 직접 호소하거나 문의한 핵심 내용만 한국어로 작성합니다.
                4. patientConsultationDetails는 1000자를 넘지 않습니다.
                5. instructions에는 의료진이 실제로 말한 지시사항과 후속조치만 행동 문장으로 작성합니다.
                6. instructions는 최대 10개이며, 각 항목은 200자를 넘지 않습니다.
                7. 불명확하거나 기록에 없는 내용은 추측하지 않습니다.
                8. 인사, 반복 발화, 자막 오류로 보이는 단편은 제외합니다.
                9. 마크다운 코드 블록을 사용하지 마세요.
                10. JSON 앞뒤에 설명, 안내 문구, 주석을 작성하지 마세요.
                11. 반드시 아래 구조의 JSON 객체 하나만 반환하세요.

                반환 형식:
                {
                  "summary": "한국어 상담 요약",
                  "patientConsultationDetails": "환자가 호소하거나 문의한 핵심 내용",
                  "instructions": [
                    "의료진 지시사항 1",
                    "의료진 지시사항 2"
                  ]
                }

                상담 기록:
                """ + transcript;
    }

    /**
     * 첫 번째 응답이 JSON 파싱에 실패했을 때 사용하는 재요청 프롬프트입니다.
     */
    private String createRetryPrompt(String originalPrompt) {
        return originalPrompt + """

                이전 응답은 올바른 JSON으로 파싱되지 않았습니다.
                이번에는 반드시 다음 조건을 지키세요.

                - JSON 객체 하나만 출력하세요.
                - ```json 코드 블록을 사용하지 마세요.
                - JSON 앞뒤에 설명을 붙이지 마세요.
                - 모든 문자열은 큰따옴표를 사용하세요.
                - 마지막 중괄호까지 완전하게 출력하세요.
                - summary, patientConsultationDetails, instructions를 모두 포함하세요.
                - instructions가 없으면 빈 배열 []을 사용하세요.
                """;
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
        if (payload == null
                || payload.summary() == null
                || payload.summary().isBlank()) {
            throw new IllegalArgumentException(
                    "Gemini 요약이 비어 있습니다."
            );
        }

        String summary = limit(
                payload.summary().trim(),
                SUMMARY_MAX_LENGTH
        );

        String patientDetails =
                payload.patientConsultationDetails() == null
                        ? ""
                        : limit(
                        payload.patientConsultationDetails().trim(),
                        PATIENT_DETAILS_MAX_LENGTH
                );

        LinkedHashSet<String> uniqueInstructions =
                new LinkedHashSet<>();

        if (payload.instructions() != null) {
            payload.instructions().stream()
                    .filter(instruction ->
                            instruction != null
                                    && !instruction.isBlank()
                    )
                    .map(String::trim)
                    .map(instruction ->
                            limit(
                                    instruction,
                                    INSTRUCTION_MAX_LENGTH
                            )
                    )
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
        int codePointCount =
                value.codePointCount(0, value.length());

        if (codePointCount <= maxLength) {
            return value;
        }

        int endIndex = value.offsetByCodePoints(
                0,
                maxLength
        );

        return value.substring(0, endIndex).trim();
    }

    private boolean isKorean(String language) {
        if (language == null) {
            return false;
        }

        String normalized =
                language.trim().toUpperCase();

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