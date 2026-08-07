package com.centerton.centerton.domain.patient.web.dto;

import com.centerton.centerton.domain.patient.entity.enums.Language;
import jakarta.validation.constraints.Pattern;

public record PatientSettingsUpdateReq(
        Language language,
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "국가 코드는 ISO 2자리 형식이어야 합니다.")
        String nationality,
        String timezoneId
) {
}
