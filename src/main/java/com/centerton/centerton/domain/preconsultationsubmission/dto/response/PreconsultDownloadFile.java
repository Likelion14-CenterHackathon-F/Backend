package com.centerton.centerton.domain.preconsultationsubmission.dto.response;

import org.springframework.core.io.Resource;

public record PreconsultDownloadFile(
        String storedFileName,
        String contentType,
        Resource resource
) {
}
