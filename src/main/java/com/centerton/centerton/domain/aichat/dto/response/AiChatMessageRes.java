package com.centerton.centerton.domain.aichat.dto.response;

import com.centerton.centerton.domain.aichat.entity.AiChatMessage;
import com.centerton.centerton.domain.aichat.entity.enums.ChatMessageRole;
import com.centerton.centerton.global.util.UtcDateTimeUtils;

import java.time.OffsetDateTime;

public record AiChatMessageRes(
        Long messageId,
        ChatMessageRole role,
        String content,
        String imageUrl,
        OffsetDateTime sentAt
) {

    public static AiChatMessageRes from(AiChatMessage message) {
        return new AiChatMessageRes(
                message.getChatMessageId(),
                message.getRole(),
                message.getContent(),
                message.getImageUrl(),
                UtcDateTimeUtils.toUtcOffset(message.getSentAt())
        );
    }
}
