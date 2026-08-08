package com.centerton.centerton.domain.appointment.dto.response;

import java.time.OffsetDateTime;

public record AppointmentDetailRes(
        Long appointmentId,
        Long caseId,
        Long slotId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        OffsetDateTime waitingRoomOpensAt,
        OffsetDateTime waitingRoomClosesAt,
        boolean canEnterWaitingRoom,
        String timezoneId
) {
}
