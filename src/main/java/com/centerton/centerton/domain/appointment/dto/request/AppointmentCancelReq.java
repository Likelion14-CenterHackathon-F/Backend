package com.centerton.centerton.domain.appointment.dto.request;

import com.centerton.centerton.domain.appointment.entity.enums.AppointmentCancelReason;
import jakarta.validation.constraints.NotNull;

public record AppointmentCancelReq(
        @NotNull AppointmentCancelReason cancelReason
) {
}
