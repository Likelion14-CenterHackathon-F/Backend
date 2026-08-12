package com.centerton.centerton.domain.aichat.entity;

import com.centerton.centerton.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "ai_chat_image_attachments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_chat_image_attachment_message",
                columnNames = "chat_message_id"
        ),
        indexes = {
                @Index(
                        name = "idx_ai_chat_image_attachments_file_url",
                        columnList = "image_url"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AiChatImageAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_attachment_id")
    private Long imageAttachmentId;

    @Column(name = "stored_file_name", nullable = false)
    private String storedFileName;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_message_id", nullable = false)
    private AiChatMessage chatMessage;

    public static AiChatImageAttachment create(
            AiChatMessage chatMessage,
            String storedFileName,
            String imageUrl,
            String originalFileName,
            String contentType,
            Long sizeBytes
    ) {
        return new AiChatImageAttachment(
                null,
                storedFileName,
                imageUrl,
                originalFileName,
                contentType,
                sizeBytes,
                chatMessage
        );
    }
}
