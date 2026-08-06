package com.centerton.centerton.domain.patient.web.dto;

public record PatientAccessLinkCreateReq(
        Integer expiresInMinutes
) {
    public static final int MIN_EXPIRES_IN_MINUTES = 1;
    public static final int MAX_EXPIRES_IN_DAYS = 30;
    public static final int MAX_EXPIRES_IN_MINUTES = MAX_EXPIRES_IN_DAYS * 24 * 60;
    public static final int DEFAULT_EXPIRES_IN_MINUTES = MAX_EXPIRES_IN_MINUTES;

    public int resolvedExpiresInMinutes() {
        return expiresInMinutes == null ? DEFAULT_EXPIRES_IN_MINUTES : expiresInMinutes;
    }
}
