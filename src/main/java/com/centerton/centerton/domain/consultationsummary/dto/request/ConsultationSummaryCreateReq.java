package com.centerton.centerton.domain.consultationsummary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsultationSummaryCreateReq(
        @NotBlank(message = "병원명은 필수입니다.")
        @Size(max = 255, message = "병원명은 255자를 넘을 수 없습니다.")
        String hospitalName,

        @NotBlank(message = "의료진명은 필수입니다.")
        @Size(max = 255, message = "의료진명은 255자를 넘을 수 없습니다.")
        String medicalStaffName,

        @NotBlank(message = "응답 언어는 필수입니다.")
        String language
) {
}
