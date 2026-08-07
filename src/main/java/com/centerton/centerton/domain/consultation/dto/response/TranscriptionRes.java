package com.centerton.centerton.domain.consultation.dto.response;

import com.centerton.centerton.domain.consultation.entity.enums.SttAgentStatus;

public record TranscriptionRes(
        Long sessionId,
        String agentId,
        SttAgentStatus status
) {
}
