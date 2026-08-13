package com.centerton.centerton.domain.aichat.storage;

public record ValidatedAiChatImage(
        String extension,
        AiChatImageType imageType
) {
}
