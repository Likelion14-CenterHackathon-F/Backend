package com.centerton.centerton.domain.aichat.repository;

import com.centerton.centerton.domain.aichat.entity.AiChatImageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiChatImageAttachmentRepository extends JpaRepository<AiChatImageAttachment, Long> {

    @Query("""
            select imageAttachment
            from AiChatImageAttachment imageAttachment
            join imageAttachment.chatMessage chatMessage
            join chatMessage.chatRoom chatRoom
            where imageAttachment.imageUrl = :imageUrl
              and chatRoom.patient.id = :patientId
            """)
    Optional<AiChatImageAttachment> findAccessibleImage(
            @Param("imageUrl") String imageUrl,
            @Param("patientId") Long patientId
    );
}
