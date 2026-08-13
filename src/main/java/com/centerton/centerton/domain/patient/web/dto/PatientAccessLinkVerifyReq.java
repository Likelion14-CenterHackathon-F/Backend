package com.centerton.centerton.domain.patient.web.dto;

import com.centerton.centerton.domain.patient.entity.enums.Language;

import java.time.LocalDate;

public record PatientAccessLinkVerifyReq(
        String token,
        LocalDate birthDate,
        Language language,
        String timezoneId
) {
}
