package com.centerton.centerton.domain.appointment.dto.response;

import java.time.OffsetDateTime;

public record AppointmentHomeSummary(
        Long appointmentId,
        OffsetDateTime startsAt
) {
}
