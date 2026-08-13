package com.centerton.centerton.domain.patient.web.dto;

import com.centerton.centerton.domain.patient.entity.Patient;
import com.centerton.centerton.domain.patient.entity.enums.Language;

public record PatientSettingsUpdateRes(
        Long patientId,
        Language language,
        String accessToken
) {

    public static PatientSettingsUpdateRes of(Patient patient, String accessToken) {
        return new PatientSettingsUpdateRes(
                patient.getId(),
                patient.getLanguage(),
                accessToken
        );
    }
}
