package com.centerton.centerton.domain.aichat.repository;

import com.centerton.centerton.domain.aichat.entity.AiChatRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiChatRoomRepository extends JpaRepository<AiChatRoom, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select chatRoom
            from AiChatRoom chatRoom
            where chatRoom.chatRoomId = :chatRoomId
              and chatRoom.patient.id = :patientId
            """)
    Optional<AiChatRoom> findByIdAndPatientIdForUpdate(
            @Param("chatRoomId") Long chatRoomId,
            @Param("patientId") Long patientId
    );
}
