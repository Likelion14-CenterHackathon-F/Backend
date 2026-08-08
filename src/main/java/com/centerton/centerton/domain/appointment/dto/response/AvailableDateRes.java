package com.centerton.centerton.domain.appointment.dto.response;

import java.time.LocalDate;

public record AvailableDateRes(
        LocalDate date,
        int availableCount
) {
}
