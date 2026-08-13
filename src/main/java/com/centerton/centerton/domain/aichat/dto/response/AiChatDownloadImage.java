package com.centerton.centerton.domain.aichat.dto.response;

import org.springframework.core.io.Resource;

public record AiChatDownloadImage(
        String storedFileName,
        String contentType,
        Resource resource
) {
}
