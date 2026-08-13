package com.centerton.centerton.domain.aichat.storage;

import com.centerton.centerton.domain.aichat.exception.AiChatErrorCode;
import com.centerton.centerton.global.exception.BaseException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;

@Component
public class AiChatImageValidator {

    public static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;

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

    private static final byte[] RIFF_SIGNATURE = {
            0x52,
            0x49,
            0x46,
            0x46
    };

    private static final byte[] WEBP_SIGNATURE = {
            0x57,
            0x45,
            0x42,
            0x50
    };

    public ValidatedAiChatImage validate(MultipartFile image) {
        validateImagePresenceAndSize(image);

        String extension = getExtension(image.getOriginalFilename());
        AiChatImageType imageType = resolveImageType(
                extension,
                image.getContentType()
        );

        if (!hasExpectedSignature(image, imageType)) {
            throw new BaseException(AiChatErrorCode.IMAGE_TYPE_UNSUPPORTED);
        }

        return new ValidatedAiChatImage(
                imageType.getStorageExtension(),
                imageType
        );
    }

    public AiChatImageType resolveStoredImageType(String storedFileName) {
        String extension = getExtension(storedFileName);

        return Arrays.stream(AiChatImageType.values())
                .filter(imageType -> imageType.supportsExtension(extension))
                .findFirst()
                .orElseThrow(() -> new BaseException(
                        AiChatErrorCode.IMAGE_TYPE_UNSUPPORTED
                ));
    }

    private void validateImagePresenceAndSize(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BaseException(AiChatErrorCode.IMAGE_EMPTY);
        }

        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BaseException(AiChatErrorCode.IMAGE_TOO_LARGE);
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null) {
            throw new BaseException(AiChatErrorCode.IMAGE_TYPE_UNSUPPORTED);
        }

        int separatorIndex = fileName.lastIndexOf('.');

        if (separatorIndex < 0 || separatorIndex == fileName.length() - 1) {
            throw new BaseException(AiChatErrorCode.IMAGE_TYPE_UNSUPPORTED);
        }

        return fileName.substring(separatorIndex + 1)
                .toLowerCase(Locale.ROOT);
    }

    private AiChatImageType resolveImageType(
            String extension,
            String contentType
    ) {
        return Arrays.stream(AiChatImageType.values())
                .filter(imageType -> imageType.supportsExtension(extension))
                .filter(imageType -> imageType.supportsContentType(contentType))
                .findFirst()
                .orElseThrow(() -> new BaseException(
                        AiChatErrorCode.IMAGE_TYPE_UNSUPPORTED
                ));
    }

    private boolean hasExpectedSignature(
            MultipartFile image,
            AiChatImageType imageType
    ) {
        try (InputStream inputStream = image.getInputStream()) {
            byte[] header = inputStream.readNBytes(16);

            return switch (imageType) {
                case JPEG -> startsWith(header, JPEG_SIGNATURE, 0);
                case PNG -> startsWith(header, PNG_SIGNATURE, 0);
                case WEBP -> startsWith(header, RIFF_SIGNATURE, 0)
                        && startsWith(header, WEBP_SIGNATURE, 8);
            };
        } catch (IOException exception) {
            throw new BaseException(AiChatErrorCode.IMAGE_STORAGE_FAILED);
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
