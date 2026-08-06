package com.centerton.centerton.domain.patient.web.dto;

import com.centerton.centerton.domain.patient.entity.PatientAccessLink;

import java.time.LocalDateTime;

public record PatientAccessLinkCreateRes(
        Long accessLinkId,
        Long patientId,
        String token,
        String magicLink,
        LocalDateTime expiresAt
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
                accessLink.getExpiresAt()
        );
    }
}
