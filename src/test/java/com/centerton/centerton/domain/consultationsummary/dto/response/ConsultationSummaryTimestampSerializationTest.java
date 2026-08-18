package com.centerton.centerton.domain.consultationsummary.dto.response;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultationSummaryTimestampSerializationTest {

    private static final String UTC_TIMESTAMP_PATTERN =
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void summaryAndNestedInstructionUseUtcTimestamps() {
        OffsetDateTime timestamp = OffsetDateTime.parse(
                "2026-08-17T10:20:00Z"
        );
        ConsultationSummaryDetailRes response =
                new ConsultationSummaryDetailRes(
                        1L,
                        timestamp,
                        "doctor",
                        900,
                        "KO",
                        "요약",
                        "상세",
                        List.of(new SummaryInstructionRes(
                                2L,
                                "안내",
                                1,
                                true,
                                timestamp.plusHours(1)
                        )),
                        3L
                );

        JsonNode root = jsonMapper.readTree(
                jsonMapper.writeValueAsString(response)
        );

        assertThat(root.path("consultedAt").asText())
                .matches(UTC_TIMESTAMP_PATTERN);
        assertThat(root.path("instructions").get(0)
                .path("completedAt").asText())
                .matches(UTC_TIMESTAMP_PATTERN);
        assertThat(root.path("instructions").get(0)
                .path("patientCompleted").asBoolean()).isTrue();
    }

    @Test
    void incompleteInstructionKeepsNullCompletionTime() {
        SummaryInstructionRes response = new SummaryInstructionRes(
                1L,
                "안내",
                1,
                false,
                null
        );

        JsonNode root = jsonMapper.readTree(
                jsonMapper.writeValueAsString(response)
        );

        assertThat(root.get("completedAt").isNull()).isTrue();
    }
}
