package com.centerton.centerton.domain.aichat.client;

import com.centerton.centerton.domain.aichat.config.AiRagServiceProperties;
import com.centerton.centerton.domain.aichat.exception.AiChatErrorCode;
import com.centerton.centerton.domain.aichat.service.AiChatAnswerMessage;
import com.centerton.centerton.domain.aichat.service.AiChatAnswerRequest;
import com.centerton.centerton.domain.aichat.service.AiChatAnswerService;
import com.centerton.centerton.global.exception.BaseException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(
        prefix = "ai-chat.answer",
        name = "provider",
        havingValue = "rag-service"
)
public class FastApiRagChatAnswerService implements AiChatAnswerService {

    private final RestClient ragRestClient;
    private final AiRagServiceProperties properties;

    public FastApiRagChatAnswerService(
            RestClient.Builder builder,
            AiRagServiceProperties properties
    ) {
        this.properties = properties;
        RestClient.Builder ragBuilder = builder.requestFactory(http11RequestFactory());
        this.ragRestClient = isBlank(properties.getBaseUrl())
                ? ragBuilder.build()
                : ragBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    /**
     * RAG 호출을 HTTP/1.1 로 고정한다.
     *
     * <p>기본 RestClient 는 JDK HttpClient 를 쓰고 h2c 업그레이드를 시도한다. uvicorn 은
     * HTTP/2 를 말하지 않아 업그레이드를 거부한 뒤 본문을 읽지 못하고 422 를 반환한다.
     */
    private static ClientHttpRequestFactory http11RequestFactory() {
        return new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
        );
    }

    @Override
    public String generateAnswer(AiChatAnswerRequest request) {
        validateConfiguration();

        try {
            JsonNode response = ragRestClient
                    .post()
                    .uri(properties.getPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createRequestBody(request))
                    .retrieve()
                    .body(JsonNode.class);

            return composeContent(response);
        } catch (BaseException exception) {
            throw exception;
        } catch (RestClientException | IllegalStateException exception) {
            throw new BaseException(AiChatErrorCode.RAG_SERVICE_RESPONSE_FAILED);
        }
    }

    private void validateConfiguration() {
        if (isBlank(properties.getBaseUrl()) || isBlank(properties.getPath())) {
            throw new BaseException(AiChatErrorCode.RAG_SERVICE_CONFIGURATION_MISSING);
        }
    }

    private Map<String, Object> createRequestBody(AiChatAnswerRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("question", request.question());
        body.put("analysisImageUrl", request.analysisImageUrl());
        body.put("previousMessages", createPreviousMessages(request.previousMessages()));
        return body;
    }

    private List<Map<String, String>> createPreviousMessages(
            List<AiChatAnswerMessage> previousMessages
    ) {
        if (previousMessages == null || previousMessages.isEmpty()) {
            return List.of();
        }

        return previousMessages.stream()
                .map(message -> Map.of(
                        "role",
                        message.role() == null ? "USER" : message.role().getValue(),
                        "content",
                        resolveText(message.content())
                ))
                .toList();
    }

    /**
     * 구조화 응답을 프론트 계약인 단일 문자열로 합친다.
     *
     * <p>{@code recommendedAction} 은 {@code rag_answer} 일 때만 덧붙인다. {@code hard_stop} 은
     * answer 와 값이 같고, {@code insufficient_evidence} 는 answer 가 이미 같은 안내를 담는다.
     */
    static String composeContent(JsonNode response) {
        if (response == null || response.isNull()) {
            throw new IllegalStateException("RAG 서비스 응답 본문이 비어 있습니다.");
        }

        String answer = textOrEmpty(response.get("answer"));
        if (answer.isBlank()) {
            throw new IllegalStateException("RAG 서비스 응답에 answer가 없습니다.");
        }

        if (!"rag_answer".equals(textOrEmpty(response.get("route")))) {
            return answer;
        }

        String recommendedAction = textOrEmpty(response.get("recommendedAction"));
        if (recommendedAction.isBlank() || answer.contains(recommendedAction)) {
            return answer;
        }
        return answer + "\n\n" + recommendedAction;
    }

    private static String textOrEmpty(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText().strip();
    }

    private String resolveText(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
