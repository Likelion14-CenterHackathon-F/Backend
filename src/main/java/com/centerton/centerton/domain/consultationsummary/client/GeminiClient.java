package com.centerton.centerton.domain.consultationsummary.client;

import com.centerton.centerton.domain.consultationsummary.config.GeminiProperties;
import com.centerton.centerton.domain.consultationsummary.exception.ConsultationSummaryErrorCode;
import com.centerton.centerton.global.exception.BaseException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private final RestClient restClient;
    private final GeminiProperties properties;

    public GeminiClient(
            @Qualifier("geminiRestClient") RestClient restClient,
            GeminiProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public String generate(String prompt) {
        validateConfiguration();

        GeminiGenerateRequest request = new GeminiGenerateRequest(
                List.of(new GeminiContent(List.of(new GeminiPart(prompt)))),
                new GeminiGenerationConfig(
                        0.2,
                        1024,
                        "application/json",
                        responseSchema()
                )
        );

        try {
            GeminiGenerateResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", properties.getModel())
                    .header("x-goog-api-key", properties.getApiKey())
                    .body(request)
                    .retrieve()
                    .body(GeminiGenerateResponse.class);

            return extractText(response);
        } catch (RestClientException | IllegalStateException exception) {
            throw new BaseException(ConsultationSummaryErrorCode.GEMINI_SUMMARY_FAILED);
        }
    }

    private void validateConfiguration() {
        if (isBlank(properties.getApiKey()) || isBlank(properties.getModel())) {
            throw new BaseException(
                    ConsultationSummaryErrorCode.GEMINI_CONFIGURATION_MISSING
            );
        }
    }

    private String extractText(GeminiGenerateResponse response) {
        if (response == null
                || response.candidates() == null
                || response.candidates().isEmpty()
                || response.candidates().getFirst().content() == null
                || response.candidates().getFirst().content().parts() == null
                || response.candidates().getFirst().content().parts().isEmpty()) {
            throw new IllegalStateException("Gemini 응답 본문이 비어 있습니다.");
        }

        String text = response.candidates().getFirst().content().parts().getFirst().text();
        if (isBlank(text)) {
            throw new IllegalStateException("Gemini 응답 텍스트가 비어 있습니다.");
        }
        return text;
    }

    private Map<String, Object> responseSchema() {
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "summary", Map.of(
                                "type", "STRING",
                                "description", "한국어 상담 요약. 공백 포함 300자 이하"
                        ),
                        "patientConsultationDetails", Map.of(
                                "type", "STRING",
                                "description", "환자가 호소하거나 문의한 핵심 내용을 한국어로 정리"
                        ),
                        "instructions", Map.of(
                                "type", "ARRAY",
                                "items", Map.of("type", "STRING"),
                                "description", "의료진이 실제로 안내한 지시 및 후속조치 목록"
                        )
                ),
                "required", List.of(
                        "summary",
                        "patientConsultationDetails",
                        "instructions"
                )
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record GeminiGenerateRequest(
            List<GeminiContent> contents,
            GeminiGenerationConfig generationConfig
    ) {
    }

    private record GeminiContent(List<GeminiPart> parts) {
    }

    private record GeminiPart(String text) {
    }

    private record GeminiGenerationConfig(
            double temperature,
            int maxOutputTokens,
            String responseMimeType,
            Map<String, Object> responseSchema
    ) {
    }

    private record GeminiGenerateResponse(List<GeminiCandidate> candidates) {
    }

    private record GeminiCandidate(GeminiResponseContent content) {
    }

    private record GeminiResponseContent(List<GeminiResponsePart> parts) {
    }

    private record GeminiResponsePart(String text) {
    }
}
