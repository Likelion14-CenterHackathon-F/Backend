package com.centerton.centerton.domain.patient.web.dto;

public record PatientAccessLinkVerifyRes(
        Long patientId,
        String accessToken
) {
    public static PatientAccessLinkVerifyRes of(
            Long patientId,
            String accessToken
    ) {
        return new PatientAccessLinkVerifyRes(patientId, accessToken);
    }
}
