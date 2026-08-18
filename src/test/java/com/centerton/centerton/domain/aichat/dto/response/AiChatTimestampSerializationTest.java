package com.centerton.centerton.domain.aichat.dto.response;

import com.centerton.centerton.domain.aichat.entity.AiChatMessage;
import com.centerton.centerton.domain.aichat.entity.AiChatRoom;
import com.centerton.centerton.domain.patient.entity.Patient;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AiChatTimestampSerializationTest {

    private static final String UTC_TIMESTAMP_PATTERN =
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void messageAndRoomFactoriesRemoveFractionalSecondsAndAddUtcOffset() {
        Patient patient = Patient.builder()
                .id(1L)
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();
        LocalDateTime sentAt = LocalDateTime.of(
                2026, 8, 17, 10, 20, 0, 123_456_789
        );
        AiChatRoom room = AiChatRoom.create(patient, "질문", sentAt);
        AiChatMessage message = room.addUserMessage("질문", sentAt);

        JsonNode messageJson = jsonMapper.readTree(
                jsonMapper.writeValueAsString(AiChatMessageRes.from(message))
        );
        JsonNode roomJson = jsonMapper.readTree(
                jsonMapper.writeValueAsString(AiChatRoomListRes.from(room))
        );

        assertThat(messageJson.path("sentAt").asText())
                .matches(UTC_TIMESTAMP_PATTERN)
                .isEqualTo("2026-08-17T10:20:00Z");
        assertThat(roomJson.path("lastMessageAt").asText())
                .matches(UTC_TIMESTAMP_PATTERN)
                .isEqualTo("2026-08-17T10:20:00Z");
        assertThat(messageJson.path("role").asText()).isEqualTo("USER");
    }
}
