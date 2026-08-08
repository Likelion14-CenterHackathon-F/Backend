package com.centerton.centerton.domain.preconsultationsubmission.storage;

import java.util.Locale;
import java.util.Set;

public enum PreconsultFileType {

    JPEG(
            Set.of("jpg", "jpeg"),
            Set.of("image/jpeg", "image/jpg"),
            "image/jpeg"
    ),
    PNG(
            Set.of("png"),
            Set.of("image/png"),
            "image/png"
    ),
    MP4(
            Set.of("mp4"),
            Set.of("video/mp4"),
            "video/mp4"
    );

    private final Set<String> extensions;
    private final Set<String> contentTypes;
    private final String responseContentType;

    PreconsultFileType(
            Set<String> extensions,
            Set<String> contentTypes,
            String responseContentType
    ) {
        this.extensions = extensions;
        this.contentTypes = contentTypes;
        this.responseContentType = responseContentType;
    }

    public boolean supportsExtension(String extension) {
        return extensions.contains(extension.toLowerCase(Locale.ROOT));
    }

    public boolean supportsContentType(String contentType) {
        return contentType != null
                && contentTypes.contains(contentType.toLowerCase(Locale.ROOT));
    }

    public String getResponseContentType() {
        return responseContentType;
    }
}
