package com.centerton.centerton.domain.aichat.storage;

public record StoredAiChatImage(
        String storedFileName,
        String imageUrl,
        String originalFileName,
        String contentType,
        Long sizeBytes
) {
}
