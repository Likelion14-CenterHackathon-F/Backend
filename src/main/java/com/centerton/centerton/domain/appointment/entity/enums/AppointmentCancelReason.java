package com.centerton.centerton.domain.appointment.entity.enums;

import com.centerton.centerton.domain.appointment.exception.AppointmentErrorCode;
import com.centerton.centerton.global.exception.BaseException;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum AppointmentCancelReason {
    OTHER;

    @JsonCreator
    public static AppointmentCancelReason from(String value) {
        if (value == null) {
            return null;
        }

        try {
            return AppointmentCancelReason.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new BaseException(
                    AppointmentErrorCode.INVALID_CANCELLATION_REASON
            );
        }
    }
}
