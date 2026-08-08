package com.centerton.centerton.domain.consultationsummary.dto.request;

import jakarta.validation.constraints.NotNull;

public record InstructionCompletionReq(
        @NotNull(message = "완료 여부는 필수입니다.")
        Boolean patientCompleted
) {
}
