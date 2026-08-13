package com.centerton.centerton.domain.aichat.service;

import com.centerton.centerton.domain.aichat.entity.AiChatMessage;
import com.centerton.centerton.domain.aichat.entity.AiChatRoom;

import java.util.List;

public record SavedAiChatUserMessage(
        AiChatRoom chatRoom,
        AiChatMessage userMessage,
        List<AiChatAnswerMessage> previousMessages
) {
}
