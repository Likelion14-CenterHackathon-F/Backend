package com.centerton.centerton.domain.consultation.dto.response;

import com.centerton.centerton.domain.consultation.entity.enums.ConsultationSessionStatus;

import java.time.OffsetDateTime;

public record ConsultationEndRes(
        Long sessionId,
        ConsultationSessionStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        Integer actualDurationSeconds
) {
}
