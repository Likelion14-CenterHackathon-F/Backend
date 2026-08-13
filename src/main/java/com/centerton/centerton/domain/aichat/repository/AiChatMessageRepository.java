package com.centerton.centerton.domain.aichat.repository;

import com.centerton.centerton.domain.aichat.entity.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    @Query("""
            select chatMessage
            from AiChatMessage chatMessage
            join chatMessage.chatRoom chatRoom
            where chatMessage.imageUrl = :imageUrl
              and chatRoom.patient.id = :patientId
            """)
    Optional<AiChatMessage> findAccessibleImageMessage(
            @Param("imageUrl") String imageUrl,
            @Param("patientId") Long patientId
    );
}
