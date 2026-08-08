package com.centerton.centerton.domain.appointment.policy;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentTimePolicyTest {

    private static final LocalDateTime STARTS_AT =
            LocalDateTime.of(2026, 8, 9, 10, 0);

    @Test
    void 대기실은_입장_시작과_종료_경계에서_입장할_수_있다() {
        LocalDateTime opensAt = AppointmentTimePolicy.waitingRoomOpensAt(STARTS_AT);
        LocalDateTime closesAt = AppointmentTimePolicy.waitingRoomClosesAt(STARTS_AT);

        assertThat(AppointmentTimePolicy.canJoin(STARTS_AT, opensAt)).isTrue();
        assertThat(AppointmentTimePolicy.canJoin(STARTS_AT, closesAt)).isTrue();
    }

    @Test
    void 대기실_입장_시간을_벗어나면_입장할_수_없다() {
        LocalDateTime beforeOpen = AppointmentTimePolicy
                .waitingRoomOpensAt(STARTS_AT)
                .minusNanos(1);
        LocalDateTime afterClose = AppointmentTimePolicy
                .waitingRoomClosesAt(STARTS_AT)
                .plusNanos(1);

        assertThat(AppointmentTimePolicy.canJoin(STARTS_AT, beforeOpen)).isFalse();
        assertThat(AppointmentTimePolicy.canJoin(STARTS_AT, afterClose)).isFalse();
    }
}
