package com.centerton.centerton.domain.appointment.policy;

import java.time.LocalDateTime;

public final class AppointmentTimePolicy {

    public static final int WAITING_ROOM_OPEN_BEFORE_MINUTES = 10;
    public static final int WAITING_ROOM_CLOSE_AFTER_MINUTES = 20;

    private AppointmentTimePolicy() {
    }

    public static LocalDateTime waitingRoomOpensAt(LocalDateTime startsAt) {
        return startsAt.minusMinutes(WAITING_ROOM_OPEN_BEFORE_MINUTES);
    }

    public static LocalDateTime waitingRoomClosesAt(LocalDateTime startsAt) {
        return startsAt.plusMinutes(WAITING_ROOM_CLOSE_AFTER_MINUTES);
    }

    public static boolean canJoin(
            LocalDateTime startsAt,
            LocalDateTime now
    ) {
        LocalDateTime opensAt = waitingRoomOpensAt(startsAt);
        LocalDateTime closesAt = waitingRoomClosesAt(startsAt);

        return !now.isBefore(opensAt) && !now.isAfter(closesAt);
    }
}
