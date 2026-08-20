package com.centerton.centerton.domain.consultationsummary.client;

import com.centerton.centerton.domain.consultationsummary.config.GeminiProperties;
import com.centerton.centerton.global.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientTest {

    private static final String GENERATE_URL =
            "https://gemini.test/v1beta/models/test-model:generateContent";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MockRestServiceServer server;
    private GeminiClient geminiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("https://gemini.test");
        server = MockRestServiceServer.bindTo(restClientBuilder).build();

        GeminiProperties properties = new GeminiProperties();
        properties.setApiKey("test-api-key");
        properties.setModel("test-model");
        properties.setMaxOutputTokens(8192);

        geminiClient = new GeminiClient(restClientBuilder.build(), properties);
    }

    @Test
    void sendsObjectInstructionSchemaAndJoinsAllTextParts() {
        server.expect(once(), requestTo(GENERATE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andExpect(jsonPath("$.generationConfig.maxOutputTokens").value(8192))
                .andExpect(jsonPath(
                        "$.generationConfig.responseSchema.properties.instructions.items.type"
                ).value("OBJECT"))
                .andExpect(jsonPath(
                        "$.generationConfig.responseSchema.properties.instructions.items.properties.content.type"
                ).value("STRING"))
                .andExpect(jsonPath(
                        "$.generationConfig.responseSchema.properties.instructions.items.properties.icon.type"
                ).value("INTEGER"))
                .andExpect(jsonPath(
                        "$.generationConfig.responseSchema.properties.instructions.items.required[0]"
                ).value("content"))
                .andExpect(jsonPath(
                        "$.generationConfig.responseSchema.properties.instructions.items.required[1]"
                ).value("icon"))
                .andRespond(withSuccess(
                        geminiResponse(
                                "STOP",
                                "{\"summary\":\"요약\",",
                                "\"patientConsultationDetails\":\"상세\",",
                                "\"instructions\":[{\"content\":\"소독\",\"icon\":1}]}"
                        ),
                        MediaType.APPLICATION_JSON
                ));

        String result = geminiClient.generate("prompt");

        assertEquals(
                "{\"summary\":\"요약\",\"patientConsultationDetails\":\"상세\","
                        + "\"instructions\":[{\"content\":\"소독\",\"icon\":1}]}",
                result
        );
        server.verify();
    }

    @Test
    void rejectsNonStopFinishReasonAndPreservesCause() {
        server.expect(once(), requestTo(GENERATE_URL))
                .andRespond(withSuccess(
                        geminiResponse(
                                "MAX_TOKENS",
                                "{\"summary\":\"잘림"
                        ),
                        MediaType.APPLICATION_JSON
                ));

        BaseException exception = assertThrows(
                BaseException.class,
                () -> geminiClient.generate("prompt")
        );

        IllegalStateException cause = assertInstanceOf(
                IllegalStateException.class,
                exception.getCause()
        );
        assertTrue(cause.getMessage().contains("MAX_TOKENS"));
        server.verify();
    }

    @Test
    void preservesHttpFailureStatusInCause() {
        server.expect(once(), requestTo(GENERATE_URL))
                .andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));

        BaseException exception = assertThrows(
                BaseException.class,
                () -> geminiClient.generate("prompt")
        );

        RestClientResponseException cause = assertInstanceOf(
                RestClientResponseException.class,
                exception.getCause()
        );
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, cause.getStatusCode());
        server.verify();
    }

    private static String geminiResponse(
            String finishReason,
            String... textParts
    ) {
        List<Map<String, String>> parts = Arrays.stream(textParts)
                .map(text -> Map.of("text", text))
                .toList();

        return OBJECT_MAPPER.writeValueAsString(
                Map.of(
                        "candidates", List.of(
                                Map.of(
                                        "content", Map.of("parts", parts),
                                        "finishReason", finishReason
                                )
                        )
                )
        );
    }
}
