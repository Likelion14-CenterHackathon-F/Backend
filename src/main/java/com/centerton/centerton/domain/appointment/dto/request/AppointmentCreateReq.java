package com.centerton.centerton.domain.appointment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AppointmentCreateReq(

        @NotNull
        @Positive
        Long caseId,

        @NotNull
        @Positive
        Long slotId
) {
}
