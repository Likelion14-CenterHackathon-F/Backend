package com.centerton.centerton.domain.aichat.dto.response;

import com.centerton.centerton.domain.aichat.entity.AiChatMessage;
import com.centerton.centerton.domain.aichat.entity.enums.ChatMessageRole;

import java.time.LocalDateTime;

public record AiChatMessageRes(
        Long messageId,
        ChatMessageRole role,
        String content,
        LocalDateTime sentAt,
        AiChatImageAttachmentRes image
) {

    public static AiChatMessageRes from(AiChatMessage message) {
        return new AiChatMessageRes(
                message.getChatMessageId(),
                message.getRole(),
                message.getContent(),
                message.getSentAt(),
                AiChatImageAttachmentRes.from(message.getImageAttachment())
        );
    }
}
