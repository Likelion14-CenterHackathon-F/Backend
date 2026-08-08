package com.centerton.centerton.domain.appointment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "reservation_slots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReservationSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Long slotId;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "availability", nullable = false)
    private Boolean availability;

    public static ReservationSlot create(
            LocalDateTime startsAt,
            LocalDateTime endsAt
    ) {
        if (startsAt == null
                || endsAt == null
                || !endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException(
                    "예약 슬롯 종료 시각은 시작 시각보다 이후여야 합니다."
            );
        }

        return new ReservationSlot(null, startsAt, endsAt, true);
    }

    public boolean hasValidTimeRange() {
        return startsAt != null
                && endsAt != null
                && endsAt.isAfter(startsAt);
    }

    public boolean isAvailable() {
        return Boolean.TRUE.equals(availability);
    }

    public void reserve() {
        this.availability = false;
    }

    public void release() {
        this.availability = true;
    }
}
