package com.centerton.centerton.domain.appointment.entity;

import com.centerton.centerton.domain.appointment.entity.enums.AppointmentCancelReason;
import com.centerton.centerton.domain.appointment.entity.enums.AppointmentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentTest {

    @Test
    void cancellationPreservesAppointmentAndRecordsHistory() {
        Appointment appointment = Appointment.create(10L, 20L, 501L);
        LocalDateTime cancelledAt = LocalDateTime.of(
                2026, 8, 14, 13, 0
        );

        appointment.cancel(AppointmentCancelReason.OTHER, cancelledAt);

        assertThat(appointment.getStatus()).isEqualTo(
                AppointmentStatus.CANCELLED
        );
        assertThat(appointment.getCancelReason()).isEqualTo(
                AppointmentCancelReason.OTHER
        );
        assertThat(appointment.getCancelledAt()).isEqualTo(cancelledAt);
        assertThat(appointment.getSlotId()).isEqualTo(501L);
    }
}
