package com.centerton.centerton.domain.consultationsummary.service;

import com.centerton.centerton.domain.consultation.entity.TranscriptSegment;
import com.centerton.centerton.domain.consultationsummary.client.GeminiClient;
import com.centerton.centerton.domain.consultationsummary.exception.ConsultationSummaryErrorCode;
import com.centerton.centerton.global.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Service
public class GeminiSummaryService {

    private static final int SUMMARY_MAX_LENGTH = 300;
    private static final int PATIENT_DETAILS_MAX_LENGTH = 2000;
    private static final int INSTRUCTION_MAX_LENGTH = 300;
    private static final int INSTRUCTION_MAX_COUNT = 10;

    /*
     * Gemini 입력이 무제한으로 커지는 것을 방지합니다.
     *
     * 정확한 tokenizer에 의존하지 않고 문자 길이를 보수적으로
     * 제한하여 상담 자막을 여러 조각으로 나눕니다.
     */
    private static final int TRANSCRIPT_CHUNK_MAX_CHARS = 12_000;

    /*
     * 부분 요약들을 다시 Gemini에 전달할 때도
     * 입력 크기가 무제한 커지지 않도록 제한합니다.
     */
    private static final int MERGE_INPUT_MAX_CHARS = 20_000;

    /*
     * 잘못된 JSON 응답에 대해서만 한 번 재시도합니다.
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
        if (transcriptSegments == null
                || transcriptSegments.isEmpty()) {

            throw new BaseException(
                    ConsultationSummaryErrorCode
                            .CONSULTATION_TRANSCRIPT_EMPTY
            );
        }

        List<String> transcriptChunks =
                splitTranscript(transcriptSegments);

        if (transcriptChunks.size() == 1) {
            return generateStructuredSummary(
                    createTranscriptPrompt(
                            transcriptChunks.get(0),
                            1,
                            1
                    )
            );
        }

        log.info(
                "긴 상담 자막 분할 요약 실행. segmentCount={}, chunkCount={}",
                transcriptSegments.size(),
                transcriptChunks.size()
        );

        List<SummaryResult> partialSummaries =
                new ArrayList<>();

        for (int index = 0;
             index < transcriptChunks.size();
             index++) {

            SummaryResult partial =
                    generateStructuredSummary(
                            createTranscriptPrompt(
                                    transcriptChunks.get(index),
                                    index + 1,
                                    transcriptChunks.size()
                            )
                    );

            partialSummaries.add(partial);
        }

        return mergePartialSummaries(partialSummaries);
    }

    /**
     * 전체 자막을 일정 길이 이하의 문자열로 분할합니다.
     *
     * 하나의 매우 긴 자막 segment가 들어오는 경우도
     * 동일한 제한을 지키도록 segment 내부 텍스트까지 분리합니다.
     */
    private List<String> splitTranscript(
            List<TranscriptSegment> transcriptSegments
    ) {
        List<String> chunks = new ArrayList<>();

        StringBuilder currentChunk =
                new StringBuilder();

        for (TranscriptSegment segment : transcriptSegments) {

            List<String> segmentParts =
                    splitTranscriptSegment(segment);

            for (String part : segmentParts) {

                if (!currentChunk.isEmpty()
                        && currentChunk.length() + part.length()
                        > TRANSCRIPT_CHUNK_MAX_CHARS) {

                    chunks.add(currentChunk.toString());

                    currentChunk =
                            new StringBuilder();
                }

                currentChunk.append(part);
            }
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    private List<String> splitTranscriptSegment(
            TranscriptSegment segment
    ) {
        String role =
                segment.getSpeakerRole() == null
                        ? "UNKNOWN"
                        : segment.getSpeakerRole().getValue();

        String prefix = "[" + role + "] ";

        String text =
                nonNull(resolveReadableText(segment));

        int maxTextLength =
                Math.max(
                        1,
                        TRANSCRIPT_CHUNK_MAX_CHARS
                                - prefix.length()
                                - 1
                );

        if (text.isEmpty()) {
            return List.of(prefix + "\n");
        }

        List<String> parts = new ArrayList<>();

        int offset = 0;

        while (offset < text.length()) {

            int endIndex =
                    safeEndIndex(
                            text,
                            offset,
                            maxTextLength
                    );

            parts.add(
                    prefix
                            + text.substring(offset, endIndex)
                            + "\n"
            );

            offset = endIndex;
        }

        return parts;
    }

    private int safeEndIndex(
            String value,
            int startIndex,
            int maxLength
    ) {
        int endIndex =
                Math.min(
                        value.length(),
                        startIndex + maxLength
                );

        if (endIndex < value.length()
                && endIndex > startIndex
                && Character.isHighSurrogate(
                value.charAt(endIndex - 1)
        )) {

            endIndex--;
        }

        if (endIndex <= startIndex) {
            return Math.min(
                    value.length(),
                    startIndex + 1
            );
        }

        return endIndex;
    }

    /**
     * 여러 자막 chunk에서 만들어진 부분 요약을
     * 최종 하나의 요약으로 병합합니다.
     *
     * 부분 요약 자체가 많아지는 경우에도 한 번에 전부
     * 요청하지 않고 여러 단계로 병합합니다.
     */
    private SummaryResult mergePartialSummaries(
            List<SummaryResult> summaries
    ) {
        List<SummaryResult> current =
                new ArrayList<>(summaries);

        int mergeRound = 1;

        while (current.size() > 1) {

            List<List<SummaryResult>> batches =
                    createMergeBatches(current);

            /*
             * 방어 코드:
             * 모든 결과가 지나치게 길어 각각 하나씩만 batch가
             * 만들어진다면 2개씩 강제로 묶어 무한 루프를 방지합니다.
             */
            if (batches.size() == current.size()) {
                batches = createPairBatches(current);
            }

            List<SummaryResult> next =
                    new ArrayList<>();

            for (List<SummaryResult> batch : batches) {

                if (batch.size() == 1) {
                    next.add(batch.get(0));
                    continue;
                }

                SummaryResult merged =
                        generateStructuredSummary(
                                createMergePrompt(
                                        batch,
                                        mergeRound
                                )
                        );

                next.add(merged);
            }

            current = next;
            mergeRound++;
        }

        return current.get(0);
    }

    private List<List<SummaryResult>> createMergeBatches(
            List<SummaryResult> summaries
    ) {
        List<List<SummaryResult>> batches =
                new ArrayList<>();

        List<SummaryResult> currentBatch =
                new ArrayList<>();

        int currentLength = 0;

        for (SummaryResult summary : summaries) {

            int estimatedLength =
                    formatPartialSummary(
                            summary,
                            currentBatch.size() + 1
                    ).length();

            if (!currentBatch.isEmpty()
                    && currentLength + estimatedLength
                    > MERGE_INPUT_MAX_CHARS) {

                batches.add(
                        List.copyOf(currentBatch)
                );

                currentBatch.clear();
                currentLength = 0;
            }

            currentBatch.add(summary);
            currentLength += estimatedLength;
        }

        if (!currentBatch.isEmpty()) {
            batches.add(
                    List.copyOf(currentBatch)
            );
        }

        return batches;
    }

    private List<List<SummaryResult>> createPairBatches(
            List<SummaryResult> summaries
    ) {
        List<List<SummaryResult>> batches =
                new ArrayList<>();

        for (int index = 0;
             index < summaries.size();
             index += 2) {

            int endIndex =
                    Math.min(
                            summaries.size(),
                            index + 2
                    );

            batches.add(
                    List.copyOf(
                            summaries.subList(
                                    index,
                                    endIndex
                            )
                    )
            );
        }

        return batches;
    }

    private String createTranscriptPrompt(
            String transcript,
            int chunkIndex,
            int totalChunks
    ) {
        String scope;

        if (totalChunks == 1) {
            scope = """
                아래 기록은 전체 상담 기록입니다.
                """;
        } else {
            scope =
                    "아래 기록은 전체 상담 중 "
                            + chunkIndex
                            + "/"
                            + totalChunks
                            + " 구간입니다.\n"
                            + "현재 구간에 실제로 존재하는 내용만 "
                            + "중간 요약하세요.\n"
                            + "다른 구간의 내용은 추측하지 마세요.\n";
        }

        return """
            당신은 피부과·성형외과 시술 후
            화상상담 기록 정리 도우미입니다.

            상담 기록에 실제로 등장한 내용만 사용하세요.
            새로운 진단, 처방, 약물 지시를 만들지 마세요.

            작성 규칙:
            1. summary는 한국어로 작성하고 공백 포함 300자를 넘지 않습니다.
            2. 환자의 현재 상태, 의료진의 설명, 향후 관찰 포인트를 정리합니다.
            3. patientConsultationDetails에는 환자가 직접 호소하거나 문의한 내용만 작성합니다.
            4. patientConsultationDetails는 2000자를 넘지 않습니다.
            5. instructions에는 의료진이 실제로 말한 지시사항과 후속조치만 작성합니다.
            6. instructions는 최대 10개입니다.
            7. 각 instruction 전체는 300자를 넘지 않습니다.
            8. 각 instruction은 반드시 "짧은 제목\\n상세 내용" 형식으로 작성합니다.
            9. 제목은 환자가 한눈에 내용을 알 수 있는 짧은 명사형으로 작성합니다.
               예: 처방약 복용, 냉찜질, 세안 주의, 경과 관찰, 다음 상담
            10. 제목과 상세 내용 모두 상담 기록에 실제로 있는 내용만 사용합니다.
            11. 불명확하거나 기록에 없는 내용은 추측하지 않습니다.
            12. 인사, 반복 발화, 자막 오류로 보이는 단편은 제외합니다.
            13. JSON 객체 하나만 반환하세요.
            14. 마크다운 코드 블록이나 추가 설명을 사용하지 마세요.

            """
                + scope
                + """

            반환 형식:
            {
              "summary": "한국어 상담 요약",
              "patientConsultationDetails": "환자가 호소하거나 문의한 핵심 내용",
              "instructions": [
                "짧은 제목\\n상담에서 실제로 언급된 상세 지시사항",
                "짧은 제목\\n상담에서 실제로 언급된 상세 지시사항"
              ]
            }

            상담 기록:
            """
                + transcript;
    }

    private String createMergePrompt(
            List<SummaryResult> summaries,
            int mergeRound
    ) {
        StringBuilder partialSummaryText =
                new StringBuilder();

        for (int index = 0;
             index < summaries.size();
             index++) {

            partialSummaryText.append(
                    formatPartialSummary(
                            summaries.get(index),
                            index + 1
                    )
            );
        }

        return """
            당신은 피부과·성형외과 화상상담의
            여러 부분 요약을 최종 상담 기록 하나로 합치는 도우미입니다.

            아래 부분 요약에 존재하는 내용만 사용하세요.
            새로운 진단, 처방, 약물, 증상, 지시를 만들지 마세요.

            병합 규칙:
            1. 중복되는 내용은 하나로 합칩니다.
            2. 서로 다른 중요한 환자 증상이나 문의는 가능한 한 유지합니다.
            3. 서로 다른 의료진 지시사항도 가능한 한 유지합니다.
            4. summary는 한국어 300자 이하로 작성합니다.
            5. patientConsultationDetails는 2000자 이하로 작성합니다.
            6. instructions는 최대 10개입니다.
            7. 각 instruction 전체는 300자를 넘지 않습니다.
            8. 각 instruction은 반드시 "짧은 제목\\n상세 내용" 형식을 유지합니다.
            9. 제목은 짧은 명사형으로 작성합니다.
               예: 처방약 복용, 냉찜질, 세안 주의, 경과 관찰, 다음 상담
            10. 부분 요약에 없는 내용은 추측하지 않습니다.
            11. JSON 객체 하나만 반환합니다.
            12. JSON 앞뒤에 설명이나 마크다운을 작성하지 않습니다.

            반환 형식:
            {
              "summary": "최종 한국어 상담 요약",
              "patientConsultationDetails": "최종 환자 상담 내용",
              "instructions": [
                "짧은 제목\\n상세 내용",
                "짧은 제목\\n상세 내용"
              ]
            }

            """
                + "병합 단계: "
                + mergeRound
                + "\n\n"
                + partialSummaryText;
    }

    private String formatPartialSummary(
            SummaryResult summary,
            int index
    ) {
        StringBuilder builder =
                new StringBuilder();

        builder.append("[부분 요약 ")
                .append(index)
                .append("]\n");

        builder.append("요약: ")
                .append(nonNull(summary.summary()))
                .append('\n');

        builder.append("환자 상담 내용: ")
                .append(nonNull(
                        summary.patientConsultationDetails()
                ))
                .append('\n');

        builder.append("의료진 지시사항:\n");

        if (summary.instructions().isEmpty()) {
            builder.append("- 없음\n");
        } else {
            for (String instruction :
                    summary.instructions()) {

                builder.append("- ")
                        .append(instruction)
                        .append('\n');
            }
        }

        builder.append('\n');

        return builder.toString();
    }

    /**
     * Gemini JSON 생성 및 파싱 재시도.
     *
     * 이 재시도는 토큰 분할과 별개이며
     * JSON 응답 형식이 깨진 경우만 처리합니다.
     */
    private SummaryResult generateStructuredSummary(
            String originalPrompt
    ) {
        Exception lastException = null;

        for (int attempt = 1;
             attempt <= MAX_GENERATION_ATTEMPTS;
             attempt++) {

            String requestPrompt =
                    attempt == 1
                            ? originalPrompt
                            : createRetryPrompt(
                            originalPrompt
                    );

            String rawResponse =
                    geminiClient.generate(
                            requestPrompt
                    );

            try {
                GeminiSummaryPayload payload =
                        parsePayload(rawResponse);

                return normalize(payload);

            } catch (
                    JacksonException
                    | IllegalArgumentException exception
            ) {
                lastException = exception;

                log.warn(
                        "Gemini 상담 요약 응답 파싱 실패. "
                                + "attempt={}, responseLength={}, "
                                + "exceptionType={}, message={}",
                        attempt,
                        rawResponse == null
                                ? 0
                                : rawResponse.length(),
                        exception.getClass()
                                .getSimpleName(),
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

    private GeminiSummaryPayload parsePayload(
            String rawResponse
    ) throws JacksonException {

        String json =
                extractCompleteJsonObject(
                        rawResponse
                );

        return objectMapper.readValue(
                json,
                GeminiSummaryPayload.class
        );
    }

    private String extractCompleteJsonObject(
            String rawResponse
    ) {
        if (rawResponse == null
                || rawResponse.isBlank()) {

            throw new IllegalArgumentException(
                    "Gemini 응답이 비어 있습니다."
            );
        }

        String response =
                rawResponse.trim();

        if (!response.isEmpty()
                && response.charAt(0) == '\uFEFF') {

            response =
                    response.substring(1).trim();
        }

        int startIndex =
                response.indexOf('{');

        if (startIndex < 0) {
            throw new IllegalArgumentException(
                    "Gemini 응답에서 JSON 시작 문자를 찾을 수 없습니다."
            );
        }

        int depth = 0;
        boolean insideString = false;
        boolean escaped = false;

        for (int index = startIndex;
             index < response.length();
             index++) {

            char current =
                    response.charAt(index);

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

        throw new IllegalArgumentException(
                "Gemini JSON 응답이 중간에 잘렸거나 닫히지 않았습니다."
        );
    }

    private String createRetryPrompt(
            String originalPrompt
    ) {
        return originalPrompt + """

                이전 응답은 올바른 JSON으로 파싱되지 않았습니다.
                이번에는 반드시 다음 조건을 지키세요.

                - JSON 객체 하나만 출력하세요.
                - ```json 코드 블록을 사용하지 마세요.
                - JSON 앞뒤에 설명을 붙이지 마세요.
                - 모든 문자열은 큰따옴표를 사용하세요.
                - 마지막 중괄호까지 완전하게 출력하세요.
                - summary를 반드시 포함하세요.
                - patientConsultationDetails를 반드시 포함하세요.
                - instructions를 반드시 포함하세요.
                - instructions가 없으면 빈 배열 []을 사용하세요.
                """;
    }

    private String resolveReadableText(
            TranscriptSegment segment
    ) {
        if (isKorean(
                segment.getSourceLanguage()
        )) {
            return segment.getSourceText();
        }

        if (isKorean(
                segment.getTargetLanguage()
        )
                && segment.getTranslatedText() != null
                && !segment.getTranslatedText().isBlank()) {

            return segment.getTranslatedText();
        }

        return segment.getSourceText();
    }

    private SummaryResult normalize(
            GeminiSummaryPayload payload
    ) {
        if (payload == null
                || payload.summary() == null
                || payload.summary().isBlank()) {

            throw new IllegalArgumentException(
                    "Gemini 요약이 비어 있습니다."
            );
        }

        String summary =
                limit(
                        payload.summary().trim(),
                        SUMMARY_MAX_LENGTH
                );

        String patientDetails =
                payload.patientConsultationDetails() == null
                        ? ""
                        : limit(
                        payload
                                .patientConsultationDetails()
                                .trim(),
                        PATIENT_DETAILS_MAX_LENGTH
                );

        LinkedHashSet<String> uniqueInstructions =
                new LinkedHashSet<>();

        if (payload.instructions() != null) {

            payload.instructions()
                    .stream()
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
                    .forEach(
                            uniqueInstructions::add
                    );
        }

        List<String> instructions =
                uniqueInstructions
                        .stream()
                        .limit(INSTRUCTION_MAX_COUNT)
                        .toList();

        return new SummaryResult(
                summary,
                patientDetails,
                instructions
        );
    }

    private String limit(
            String value,
            int maxLength
    ) {
        int codePointCount =
                value.codePointCount(
                        0,
                        value.length()
                );

        if (codePointCount <= maxLength) {
            return value;
        }

        int endIndex =
                value.offsetByCodePoints(
                        0,
                        maxLength
                );

        return value
                .substring(0, endIndex)
                .trim();
    }

    private boolean isKorean(
            String language
    ) {
        if (language == null) {
            return false;
        }

        String normalized =
                language.trim()
                        .toUpperCase();

        return normalized.startsWith("KO")
                || normalized.equals("KOREAN")
                || normalized.equals("한국어");
    }

    private String nonNull(
            String value
    ) {
        return value == null
                ? ""
                : value;
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