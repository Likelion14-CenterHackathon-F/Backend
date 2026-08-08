package com.centerton.centerton.domain.appointment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AppointmentChangeReq(

        @NotNull
        @Positive
        Long slotId
) {
}
