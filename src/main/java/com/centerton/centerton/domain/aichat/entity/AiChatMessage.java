package com.centerton.centerton.domain.aichat.entity;

import com.centerton.centerton.domain.aichat.entity.enums.ChatMessageRole;
import com.centerton.centerton.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.CascadeType.ALL;

@Getter
@Entity
@Table(
        name = "ai_chat_messages",
        indexes = {
                @Index(
                        name = "idx_ai_chat_messages_room_sent_at",
                        columnList = "chat_room_id, sent_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AiChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long chatMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ChatMessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private AiChatRoom chatRoom;

    @OneToOne(
            mappedBy = "chatMessage",
            cascade = ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private AiChatImageAttachment imageAttachment;

    public static AiChatMessage create(
            AiChatRoom chatRoom,
            ChatMessageRole role,
            String content,
            LocalDateTime sentAt
    ) {
        return new AiChatMessage(
                null,
                role,
                content,
                sentAt,
                chatRoom,
                null
        );
    }

    public void attachImage(
            String storedFileName,
            String imageUrl,
            String originalFileName,
            String contentType,
            Long sizeBytes
    ) {
        this.imageAttachment = AiChatImageAttachment.create(
                this,
                storedFileName,
                imageUrl,
                originalFileName,
                contentType,
                sizeBytes
        );
    }

    public boolean isUserMessage() {
        return role == ChatMessageRole.USER;
    }

    public boolean isAssistantMessage() {
        return role == ChatMessageRole.ASSISTANT;
    }
}
