package com.centerton.centerton.domain.appointment.dto.response;

import com.centerton.centerton.domain.aftercare.dto.response.AftercareHomeRes;
import com.centerton.centerton.global.util.UtcDateTimeUtils;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentTimestampSerializationTest {

    private static final String UTC_TIMESTAMP_PATTERN =
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void slotTimestampsUseUtcWhileAvailableDateRemainsDateOnly() {
        AvailableSlotListRes response = new AvailableSlotListRes(
                LocalDate.of(2026, 8, 17),
                1,
                "UTC",
                List.of(new AvailableSlotRes(
                        1L,
                        UtcDateTimeUtils.toUtcOffset(LocalDateTime.of(
                                2026, 8, 17, 10, 20, 0, 123_000_000
                        )),
                        UtcDateTimeUtils.toUtcOffset(LocalDateTime.of(
                                2026, 8, 17, 10, 35, 0, 456_000_000
                        )),
                        true
                ))
        );

        JsonNode root = jsonMapper.readTree(
                jsonMapper.writeValueAsString(response)
        );

        assertThat(root.path("date").asText()).isEqualTo("2026-08-17");
        assertThat(root.path("date").asText()).doesNotContain("T", "Z");
        assertThat(root.path("slots").get(0).path("startsAt").asText())
                .matches(UTC_TIMESTAMP_PATTERN);
        assertThat(root.path("slots").get(0).path("endsAt").asText())
                .matches(UTC_TIMESTAMP_PATTERN);
        assertThat(root.path("timezoneId").asText()).isEqualTo("UTC");
    }

    @Test
    void aftercareNestedAppointmentKeepsUtcTimestampAndStructure() {
        AftercareHomeRes.ConsultationAppointment response =
                new AftercareHomeRes.ConsultationAppointment(
                        1L,
                        UtcDateTimeUtils.toUtcOffset(LocalDateTime.of(
                                2026, 8, 17, 10, 20, 0, 999_000_000
                        ))
                );

        JsonNode root = jsonMapper.readTree(
                jsonMapper.writeValueAsString(response)
        );

        assertThat(root.path("appointmentId").asLong()).isEqualTo(1L);
        assertThat(root.path("startsAt").asText())
                .matches(UTC_TIMESTAMP_PATTERN);
    }
}
