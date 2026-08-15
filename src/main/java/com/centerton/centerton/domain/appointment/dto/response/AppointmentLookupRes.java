package com.centerton.centerton.domain.appointment.dto.response;

import com.centerton.centerton.domain.appointment.entity.enums.AppointmentStatus;
import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;

import java.time.OffsetDateTime;

public record AppointmentLookupRes(
        Long appointmentId,
        Long caseId,
        Long slotId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        SymptomCategory symptomCategory,
        String symptomNote,
        AppointmentStatus status,
        OffsetDateTime waitingRoomOpensAt,
        OffsetDateTime waitingRoomClosesAt,
        boolean canEnterWaitingRoom,
        String timezoneId
) {
}
