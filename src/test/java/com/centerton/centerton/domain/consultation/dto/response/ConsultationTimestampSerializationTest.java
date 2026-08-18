package com.centerton.centerton.domain.consultation.dto.response;

import com.centerton.centerton.domain.appointment.entity.enums.AppointmentCancelReason;
import com.centerton.centerton.domain.appointment.entity.enums.AppointmentStatus;
import com.centerton.centerton.domain.consultation.entity.enums.ConsultationSessionStatus;
import com.centerton.centerton.domain.consultation.entity.enums.ParticipantRole;
import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultationTimestampSerializationTest {

    private static final String UTC_TIMESTAMP_PATTERN =
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void historySerializesEveryTimeFieldWithUtcOffset() {
        OffsetDateTime timestamp = OffsetDateTime.parse(
                "2026-08-17T10:20:00Z"
        );
        ConsultationHistoryRes response = new ConsultationHistoryRes(
                1L,
                2L,
                timestamp,
                timestamp.plusMinutes(15),
                900,
                true,
                timestamp,
                timestamp.plusMinutes(15),
                SymptomCategory.SWELLING,
                List.of(SymptomCategory.SWELLING),
                "붓기",
                AppointmentStatus.COMPLETED,
                AppointmentCancelReason.OTHER,
                timestamp.minusMinutes(5)
        );

        JsonNode root = jsonMapper.readTree(
                jsonMapper.writeValueAsString(response)
        );

        assertUtcTimestamp(root, "startedAt");
        assertUtcTimestamp(root, "endedAt");
        assertUtcTimestamp(root, "appointmentStartsAt");
        assertUtcTimestamp(root, "appointmentEndsAt");
        assertUtcTimestamp(root, "cancelledAt");
        assertThat(root.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(root.path("symptomCategories").get(0).asText())
                .isEqualTo("붓기");
    }

    @Test
    void captionKeepsNumericProtocolFieldsAndUtcCreatedAt() {
        CaptionRes response = new CaptionRes(
                1L,
                2L,
                3,
                ParticipantRole.PATIENT,
                1001,
                "ko",
                "안녕하세요",
                "en",
                "hello",
                true,
                123456789L,
                1500,
                OffsetDateTime.parse("2026-08-17T10:20:00Z")
        );

        JsonNode root = jsonMapper.readTree(
                jsonMapper.writeValueAsString(response)
        );

        assertUtcTimestamp(root, "createdAt");
        assertThat(root.path("textTimestamp").asLong())
                .isEqualTo(123456789L);
        assertThat(root.path("durationMs").asInt()).isEqualTo(1500);
        assertThat(root.path("isFinal").asBoolean()).isTrue();
    }

    @Test
    void nullEndTimestampRemainsNull() {
        ConsultationEndRes response = new ConsultationEndRes(
                1L,
                ConsultationSessionStatus.IN_PROGRESS,
                OffsetDateTime.parse("2026-08-17T10:20:00Z"),
                null,
                null
        );

        JsonNode root = jsonMapper.readTree(
                jsonMapper.writeValueAsString(response)
        );

        assertUtcTimestamp(root, "startedAt");
        assertThat(root.get("endedAt").isNull()).isTrue();
        assertThat(root.get("actualDurationSeconds").isNull()).isTrue();
    }

    @Test
    void instantTokenFieldsKeepSecondPrecisionUtcFormat() {
        JoinConsultationRes response = new JoinConsultationRes(
                1L,
                2L,
                "app-id",
                "channel",
                1001,
                "token",
                Instant.parse("2026-08-17T10:20:00Z"),
                ParticipantRole.PATIENT,
                "ko",
                "en",
                9002,
                900,
                Instant.parse("2026-08-17T10:35:00Z")
        );

        JsonNode root = jsonMapper.readTree(
                jsonMapper.writeValueAsString(response)
        );

        assertUtcTimestamp(root, "tokenExpiresAt");
        assertUtcTimestamp(root, "forceEndAt");
        assertThat(root.path("agoraUid").asInt()).isEqualTo(1001);
    }

    private void assertUtcTimestamp(JsonNode root, String fieldName) {
        assertThat(root.path(fieldName).asText())
                .matches(UTC_TIMESTAMP_PATTERN);
    }
}
