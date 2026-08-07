package com.centerton.centerton.domain.patient.web.dto;

import com.centerton.centerton.domain.patient.entity.enums.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PatientSettingsUpdateReq(
        @NotNull(message = "사용 언어는 필수입니다.")
        Language language,

        @NotBlank(message = "국가 코드는 필수입니다.")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "국가 코드는 ISO 2자리 형식이어야 합니다.")
        String nationality,

        @NotBlank(message = "시간대는 필수입니다.")
        String timezoneId
) {
}
