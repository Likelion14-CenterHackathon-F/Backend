package com.centerton.centerton.global.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UtcDateTimeUtilsTest {

    @Test
    void convertsLocalDateTimeAsUtcAndRemovesFractionalSeconds() {
        OffsetDateTime result = UtcDateTimeUtils.toUtcOffset(
                LocalDateTime.of(2026, 8, 17, 10, 20, 0, 123_456_789)
        );

        assertThat(result.toString()).isEqualTo("2026-08-17T10:20Z");
        assertThat(result.getNano()).isZero();
        assertThat(result.getOffset().getId()).isEqualTo("Z");
    }

    @Test
    void preservesNullTimestamps() {
        assertThat(UtcDateTimeUtils.toUtcOffset(null)).isNull();
        assertThat(UtcDateTimeUtils.truncateToSeconds(null)).isNull();
    }

    @Test
    void removesFractionalSecondsFromInstant() {
        Instant result = UtcDateTimeUtils.truncateToSeconds(
                Instant.parse("2026-08-17T10:20:00.123456Z")
        );

        assertThat(result.toString()).isEqualTo("2026-08-17T10:20:00Z");
    }
}
