package com.centerton.centerton.domain.patient.web.dto;

import com.centerton.centerton.domain.patient.entity.PatientAccessLink;
import com.centerton.centerton.global.util.UtcDateTimeUtils;

import java.time.OffsetDateTime;

public record PatientAccessLinkCreateRes(
        Long accessLinkId,
        Long patientId,
        String token,
        String magicLink,
        OffsetDateTime expiresAt
) {
    public static PatientAccessLinkCreateRes of(
            PatientAccessLink accessLink,
            String token,
            String magicLink
    ) {
        return new PatientAccessLinkCreateRes(
                accessLink.getId(),
                accessLink.getPatient().getId(),
                token,
                magicLink,
                UtcDateTimeUtils.toUtcOffset(accessLink.getExpiresAt())
        );
    }
}
