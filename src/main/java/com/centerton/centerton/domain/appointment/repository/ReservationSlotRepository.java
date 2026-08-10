package com.centerton.centerton.domain.appointment.repository;

import com.centerton.centerton.domain.appointment.entity.ReservationSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationSlotRepository extends JpaRepository<ReservationSlot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from ReservationSlot slot where slot.slotId = :slotId")
    Optional<ReservationSlot> findByIdForUpdate(
            @Param("slotId") Long slotId
    );

    List<ReservationSlot> findAllByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            LocalDateTime startsAtFrom,
            LocalDateTime startsAtTo
    );
}
