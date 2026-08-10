package com.centerton.centerton.domain.preconsultationsubmission.storage;

import com.centerton.centerton.domain.preconsultationsubmission.exception.PreconsultSubmissionErrorCode;
import com.centerton.centerton.global.exception.BaseException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;

@Component
public class PreconsultFileValidator {

    public static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;
    public static final int MAX_FILE_COUNT = 5;

    private static final byte[] JPEG_SIGNATURE = {
            (byte) 0xFF,
            (byte) 0xD8,
            (byte) 0xFF
    };

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89,
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A
    };

    private static final byte[] MP4_FILE_TYPE_BOX = {
            0x66,
            0x74,
            0x79,
            0x70
    };

    public ValidatedPreconsultFile validate(MultipartFile file) {
        validateFilePresenceAndSize(file);

        String extension = getExtension(file.getOriginalFilename());
        PreconsultFileType fileType = resolveFileType(
                extension,
                file.getContentType()
        );

        if (!hasExpectedSignature(file, fileType)) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.FILE_TYPE_UNSUPPORTED
            );
        }

        return new ValidatedPreconsultFile(extension, fileType);
    }

    public PreconsultFileType resolveStoredFileType(String storedFileName) {
        String extension = getExtension(storedFileName);

        return Arrays.stream(PreconsultFileType.values())
                .filter(fileType -> fileType.supportsExtension(extension))
                .findFirst()
                .orElseThrow(() -> new BaseException(
                        PreconsultSubmissionErrorCode.FILE_TYPE_UNSUPPORTED
                ));
    }

    private void validateFilePresenceAndSize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.FILE_EMPTY
            );
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.FILE_TOO_LARGE
            );
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.FILE_TYPE_UNSUPPORTED
            );
        }

        int separatorIndex = fileName.lastIndexOf('.');

        if (separatorIndex < 0 || separatorIndex == fileName.length() - 1) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.FILE_TYPE_UNSUPPORTED
            );
        }

        return fileName.substring(separatorIndex + 1)
                .toLowerCase(Locale.ROOT);
    }

    private PreconsultFileType resolveFileType(
            String extension,
            String contentType
    ) {
        return Arrays.stream(PreconsultFileType.values())
                .filter(fileType -> fileType.supportsExtension(extension))
                .filter(fileType -> fileType.supportsContentType(contentType))
                .findFirst()
                .orElseThrow(() -> new BaseException(
                        PreconsultSubmissionErrorCode.FILE_TYPE_UNSUPPORTED
                ));
    }

    private boolean hasExpectedSignature(
            MultipartFile file,
            PreconsultFileType fileType
    ) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(12);

            return switch (fileType) {
                case JPEG -> startsWith(header, JPEG_SIGNATURE, 0);
                case PNG -> startsWith(header, PNG_SIGNATURE, 0);
                case MP4 -> startsWith(header, MP4_FILE_TYPE_BOX, 4);
            };
        } catch (IOException exception) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.FILE_STORAGE_FAILED
            );
        }
    }

    private boolean startsWith(
            byte[] source,
            byte[] expected,
            int offset
    ) {
        if (source.length < offset + expected.length) {
            return false;
        }

        for (int index = 0; index < expected.length; index++) {
            if (source[offset + index] != expected[index]) {
                return false;
            }
        }

        return true;
    }
}
