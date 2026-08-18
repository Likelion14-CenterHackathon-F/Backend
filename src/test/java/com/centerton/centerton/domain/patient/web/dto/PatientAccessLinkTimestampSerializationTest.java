package com.centerton.centerton.domain.patient.web.dto;

import com.centerton.centerton.domain.patient.entity.Patient;
import com.centerton.centerton.domain.patient.entity.PatientAccessLink;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PatientAccessLinkTimestampSerializationTest {

    private static final String UTC_TIMESTAMP_PATTERN =
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void accessLinkFactorySerializesExpiryAsUtcWithoutFractionalSeconds() {
        Patient patient = Patient.builder()
                .id(1L)
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();
        PatientAccessLink accessLink = PatientAccessLink.builder()
                .id(2L)
                .patient(patient)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.of(
                        2026, 8, 17, 10, 20, 0, 987_654_321
                ))
                .build();

        PatientAccessLinkCreateRes response = PatientAccessLinkCreateRes.of(
                accessLink,
                "token",
                "https://example.com/access?token=token"
        );
        JsonNode root = jsonMapper.readTree(
                jsonMapper.writeValueAsString(response)
        );

        assertThat(root.path("expiresAt").asText())
                .matches(UTC_TIMESTAMP_PATTERN)
                .isEqualTo("2026-08-17T10:20:00Z");
        assertThat(root.path("token").asText()).isEqualTo("token");
        assertThat(root.path("magicLink").asText())
                .isEqualTo("https://example.com/access?token=token");
    }
}
