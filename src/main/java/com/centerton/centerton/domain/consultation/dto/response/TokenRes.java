package com.centerton.centerton.domain.consultation.dto.response;

import java.time.Instant;

public record TokenRes(
        String rtcToken,
        Instant tokenExpiresAt
) {
}
