package com.centerton.centerton.domain.appointment.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AvailableSlotListRes(
        LocalDate date,
        int availableCount,
        String timezoneId,
        List<AvailableSlotRes> slots
) {
}
