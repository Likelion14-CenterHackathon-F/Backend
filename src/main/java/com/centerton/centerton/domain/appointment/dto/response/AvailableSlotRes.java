package com.centerton.centerton.domain.appointment.dto.response;

import java.time.OffsetDateTime;

public record AvailableSlotRes(
        Long slotId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        boolean available
) {
}
