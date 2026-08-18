package com.centerton.centerton.global.response;

import com.centerton.centerton.global.response.code.ErrorResponseCode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class BaseResponseTimestampSerializationTest {

    private static final String UTC_TIMESTAMP_PATTERN =
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void successResponseKeepsStructureAndUsesUtcTimestamp() {
        String json = jsonMapper.writeValueAsString(
                SuccessResponse.from("payload")
        );
        JsonNode root = jsonMapper.readTree(json);

        assertThat(root.path("timestamp").asText())
                .matches(UTC_TIMESTAMP_PATTERN);
        assertThat(json).startsWith("{\"isSuccess\":true,\"timestamp\":");
        assertThat(root.path("code").asText()).isEqualTo("GLOBAL_200");
        assertThat(root.path("data").asText()).isEqualTo("payload");
    }

    @Test
    void errorResponseKeepsStructureAndUsesUtcTimestamp() {
        String json = jsonMapper.writeValueAsString(
                ErrorResponse.from(ErrorResponseCode.BAD_REQUEST_ERROR)
        );
        JsonNode root = jsonMapper.readTree(json);

        assertThat(root.path("timestamp").asText())
                .matches(UTC_TIMESTAMP_PATTERN);
        assertThat(json).startsWith("{\"isSuccess\":false,\"timestamp\":");
        assertThat(root.path("code").asText()).isEqualTo("GLOBAL_400_1");
        assertThat(root.get("data").isNull()).isTrue();
    }
}
