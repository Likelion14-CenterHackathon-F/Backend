package com.centerton.centerton.domain.consultation.dto.response;

import com.centerton.centerton.domain.consultation.entity.ConsultationSessionStatus;

import java.time.LocalDateTime;

public record ConsultationEndRes(
        Long sessionId,
        ConsultationSessionStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer actualDurationSeconds
) {
}
