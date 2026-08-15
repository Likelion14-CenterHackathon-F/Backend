package com.centerton.centerton.domain.appointment.policy;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentTimePolicyTest {

    private final LocalDateTime now = LocalDateTime.of(
            2026, 8, 14, 12, 30
    );

    @Test
    void reservationIsRejectedAtFiftyNineMinutes() {
        assertThat(AppointmentTimePolicy.canReserve(
                now.plusMinutes(59),
                now
        )).isFalse();
    }

    @Test
    void reservationIsAllowedAtExactlySixtyMinutes() {
        assertThat(AppointmentTimePolicy.canReserve(
                now.plusMinutes(60),
                now
        )).isTrue();
    }

    @Test
    void reservationIsAllowedAtSixtyOneMinutes() {
        assertThat(AppointmentTimePolicy.canReserve(
                now.plusMinutes(61),
                now
        )).isTrue();
    }

    @Test
    void cancellationIsRejectedAtFiftyNineMinutes() {
        assertThat(AppointmentTimePolicy.canCancel(
                now.plusMinutes(59),
                now
        )).isFalse();
    }

    @Test
    void cancellationIsAllowedAtExactlySixtyMinutes() {
        assertThat(AppointmentTimePolicy.canCancel(
                now.plusMinutes(60),
                now
        )).isTrue();
    }

    @Test
    void cancellationIsAllowedAtSixtyOneMinutes() {
        assertThat(AppointmentTimePolicy.canCancel(
                now.plusMinutes(61),
                now
        )).isTrue();
    }
}
