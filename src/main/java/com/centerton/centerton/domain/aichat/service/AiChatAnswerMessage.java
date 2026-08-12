package com.centerton.centerton.domain.aichat.service;

import com.centerton.centerton.domain.aichat.entity.enums.ChatMessageRole;

public record AiChatAnswerMessage(
        ChatMessageRole role,
        String content
) {
}
