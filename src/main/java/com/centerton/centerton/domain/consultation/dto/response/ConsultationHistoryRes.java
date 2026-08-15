package com.centerton.centerton.domain.consultation.dto.response;

import com.centerton.centerton.domain.appointment.entity.enums.AppointmentCancelReason;
import com.centerton.centerton.domain.appointment.entity.enums.AppointmentStatus;
import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;

import java.time.LocalDateTime;
import java.util.List;

public record ConsultationHistoryRes(
        Long appointmentId,
        Long sessionId,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer actualDurationSeconds,
        boolean hasTranscript,
        LocalDateTime appointmentStartsAt,
        LocalDateTime appointmentEndsAt,
        SymptomCategory symptomCategory,
        List<SymptomCategory> symptomCategories,
        String symptomNote,
        AppointmentStatus status,
        AppointmentCancelReason cancelReason,
        LocalDateTime cancelledAt
) {
}
