package com.centerton.centerton.domain.aichat.storage;

import java.util.Set;

public enum AiChatImageType {
    JPEG(
            Set.of("jpg", "jpeg"),
            Set.of("image/jpeg"),
            "image/jpeg",
            "jpg"
    ),
    PNG(
            Set.of("png"),
            Set.of("image/png"),
            "image/png",
            "png"
    ),
    WEBP(
            Set.of("webp"),
            Set.of("image/webp"),
            "image/webp",
            "webp"
    );

    private final Set<String> extensions;
    private final Set<String> contentTypes;
    private final String responseContentType;
    private final String storageExtension;

    AiChatImageType(
            Set<String> extensions,
            Set<String> contentTypes,
            String responseContentType,
            String storageExtension
    ) {
        this.extensions = extensions;
        this.contentTypes = contentTypes;
        this.responseContentType = responseContentType;
        this.storageExtension = storageExtension;
    }

    public boolean supportsExtension(String extension) {
        return extension != null && extensions.contains(extension.toLowerCase());
    }

    public boolean supportsContentType(String contentType) {
        return contentType != null && contentTypes.contains(contentType.toLowerCase());
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public String getStorageExtension() {
        return storageExtension;
    }
}
