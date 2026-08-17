package com.centerton.centerton.domain.aichat.client;

import com.centerton.centerton.domain.aichat.config.OpenAiProperties;
import com.centerton.centerton.domain.aichat.entity.enums.ChatMessageRole;
import com.centerton.centerton.domain.aichat.exception.AiChatErrorCode;
import com.centerton.centerton.domain.aichat.service.AiChatAnswerMessage;
import com.centerton.centerton.domain.aichat.service.AiChatAnswerRequest;
import com.centerton.centerton.domain.aichat.service.AiChatAnswerService;
import com.centerton.centerton.global.exception.BaseException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(
        prefix = "ai-chat.answer",
        name = "provider",
        havingValue = "openai",
        matchIfMissing = true
)
public class OpenAiChatAnswerService implements AiChatAnswerService {

    private static final int MAX_HISTORY_MESSAGES = 10;
    private static final int MAX_HISTORY_CONTENT_LENGTH = 500;

    private static final String DEVELOPER_PROMPT = """
            당신은 피부과/성형외과 시술 후 사후관리 챗봇입니다.
            사용자가 보낸 증상 질문과 선택 첨부 이미지를 바탕으로 한국어로 답변하세요.
            사진만으로 진단을 확정하지 말고, 처방·복약 변경·시술 필요 여부를 단정하지 마세요.
            출혈이 멈추지 않음, 호흡곤란, 의식 저하, 급격한 부종, 심한 통증, 고름/악취, 고열처럼 위험 신호가 의심되면 즉시 병원 또는 응급실 연락을 안내하세요.
            답변은 1) 관찰되는/입력된 상태 요약, 2) 가능한 관리 방향, 3) 병원에 연락해야 하는 경우 순서로 간결하게 작성하세요.
            의료진 진료를 대체하지 않는다는 문장을 마지막에 포함하세요.
            """;

    private final RestClient openAiRestClient;
    private final OpenAiProperties properties;

    public OpenAiChatAnswerService(
            @Qualifier("openAiRestClient")
            RestClient openAiRestClient,
            OpenAiProperties properties
    ) {
        this.openAiRestClient = openAiRestClient;
        this.properties = properties;
    }

    @Override
    public String generateAnswer(AiChatAnswerRequest request) {
        validateConfiguration();

        try {
            JsonNode response = openAiRestClient
                    .post()
                    .uri("/v1/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + properties.getApiKey()
                    )
                    .body(createRequestBody(request))
                    .retrieve()
                    .body(JsonNode.class);

            return extractOutputText(response);
        } catch (BaseException exception) {
            throw exception;
        } catch (RestClientException | IllegalStateException exception) {
            throw new BaseException(AiChatErrorCode.OPENAI_RESPONSE_FAILED);
        }
    }

    private void validateConfiguration() {
        if (isBlank(properties.getApiKey()) || isBlank(properties.getModel())) {
            throw new BaseException(AiChatErrorCode.OPENAI_CONFIGURATION_MISSING);
        }
    }

    private Map<String, Object> createRequestBody(AiChatAnswerRequest request) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", properties.getModel());
        requestBody.put("input", createInput(request));
        requestBody.put("max_output_tokens", properties.getMaxOutputTokens());

        return requestBody;
    }

    private List<Map<String, Object>> createInput(AiChatAnswerRequest request) {
        List<Map<String, Object>> input = new ArrayList<>();

        input.add(Map.of(
                "role",
                "developer",
                "content",
                List.of(Map.of(
                        "type",
                        "input_text",
                        "text",
                        DEVELOPER_PROMPT
                ))
        ));

        List<Map<String, Object>> userContent = new ArrayList<>();
        userContent.add(Map.of(
                "type",
                "input_text",
                "text",
                createUserPrompt(request)
        ));

        if (request.hasImage()) {
            userContent.add(Map.of(
                    "type",
                    "input_image",
                    "image_url",
                    request.analysisImageUrl(),
                    "detail",
                    "high"
            ));
        }

        input.add(Map.of(
                "role",
                "user",
                "content",
                userContent
        ));

        return input;
    }

    private String createUserPrompt(AiChatAnswerRequest request) {
        StringBuilder prompt = new StringBuilder();

        if (request.previousMessages() != null
                && !request.previousMessages().isEmpty()) {
            prompt.append("이전 대화:\n");

            List<AiChatAnswerMessage> recentMessages = request.previousMessages()
                    .stream()
                    .skip(Math.max(0, request.previousMessages().size() - MAX_HISTORY_MESSAGES))
                    .toList();

            for (AiChatAnswerMessage message : recentMessages) {
                prompt.append(resolveRoleLabel(message.role()))
                        .append(": ")
                        .append(truncate(message.content(), MAX_HISTORY_CONTENT_LENGTH))
                        .append('\n');
            }
            prompt.append('\n');
        }

        prompt.append("현재 문의:\n")
                .append(request.question());

        if (request.hasImage()) {
            prompt.append("\n\n첨부된 이미지를 함께 참고해 주세요.");
        }

        return prompt.toString();
    }

    private String extractOutputText(JsonNode response) {
        if (response == null || response.isNull()) {
            throw new IllegalStateException("OpenAI 응답 본문이 비어 있습니다.");
        }

        JsonNode directText = response.get("output_text");
        if (directText != null && !directText.isNull() && !directText.asText().isBlank()) {
            return directText.asText();
        }

        StringBuilder outputText = new StringBuilder();
        JsonNode output = response.path("output");

        if (output.isArray()) {
            for (JsonNode outputItem : output) {
                JsonNode content = outputItem.path("content");

                if (!content.isArray()) {
                    continue;
                }

                for (JsonNode contentItem : content) {
                    JsonNode text = contentItem.get("text");
                    if (text != null && !text.isNull() && !text.asText().isBlank()) {
                        if (!outputText.isEmpty()) {
                            outputText.append('\n');
                        }
                        outputText.append(text.asText());
                    }
                }
            }
        }

        if (outputText.isEmpty()) {
            throw new IllegalStateException("OpenAI 응답 텍스트가 비어 있습니다.");
        }

        return outputText.toString();
    }

    private String resolveRoleLabel(ChatMessageRole role) {
        if (role == ChatMessageRole.ASSISTANT) {
            return "AI";
        }
        return "환자";
    }

    private String truncate(
            String content,
            int maxLength
    ) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
