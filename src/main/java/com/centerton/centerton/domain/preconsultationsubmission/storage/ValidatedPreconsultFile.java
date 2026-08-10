package com.centerton.centerton.domain.preconsultationsubmission.storage;

public record ValidatedPreconsultFile(
        String extension,
        PreconsultFileType fileType
) {
}
