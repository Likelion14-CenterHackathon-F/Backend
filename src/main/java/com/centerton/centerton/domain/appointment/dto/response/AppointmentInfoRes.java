package com.centerton.centerton.domain.appointment.dto.response;

import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;

import java.time.OffsetDateTime;
import java.util.List;

public record AppointmentInfoRes(
        Long appointmentId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        List<SymptomCategory> symptomCategories,
        String symptomNote
) {
}
