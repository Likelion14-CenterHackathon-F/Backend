package com.centerton.centerton.global.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public final class UtcDateTimeUtils {

    private UtcDateTimeUtils() {
    }

    public static OffsetDateTime toUtcOffset(LocalDateTime value) {
        if (value == null) {
            return null;
        }

        return value.withNano(0).atOffset(ZoneOffset.UTC);
    }

    public static Instant truncateToSeconds(Instant value) {
        if (value == null) {
            return null;
        }

        return value.truncatedTo(ChronoUnit.SECONDS);
    }
}
