package com.centerton.centerton.domain.aichat.storage;

import com.centerton.centerton.global.storage.s3.S3ObjectStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3AiChatImageStorageTest {

    @Mock
    private S3ObjectStorage objectStorage;

    private S3AiChatImageStorage storage;

    @BeforeEach
    void setUp() {
        storage = new S3AiChatImageStorage(
                objectStorage,
                new AiChatImageValidator()
        );
    }

    @Test
    void storesJpegWithDomainPrefixAndInternalDisplayUrl() {
        assertStoredImage(
                "photo.jpeg",
                "image/jpeg",
                jpegBytes(),
                "jpg",
                "image/jpeg"
        );
    }

    @Test
    void storesPngWithDomainPrefixAndInternalDisplayUrl() {
        assertStoredImage(
                "photo.png",
                "image/png",
                pngBytes(),
                "png",
                "image/png"
        );
    }

    @Test
    void storesWebpWithDomainPrefixAndInternalDisplayUrl() {
        assertStoredImage(
                "photo.webp",
                "image/webp",
                webpBytes(),
                "webp",
                "image/webp"
        );
    }

    @Test
    void preservesBase64DataUrlForAnalysis() {
        byte[] bytes = jpegBytes();
        when(objectStorage.readAllBytes("ai-chat-images/stored.jpg"))
                .thenReturn(bytes);

        String analysisUrl = storage.resolveAnalysisImageUrl("stored.jpg");

        assertThat(analysisUrl).isEqualTo(
                "data:image/jpeg;base64,"
                        + Base64.getEncoder().encodeToString(bytes)
        );
    }

    @Test
    void deletesUsingTheSameDomainObjectKey() {
        storage.delete("stored.webp");

        verify(objectStorage).delete("ai-chat-images/stored.webp");
    }

    private void assertStoredImage(
            String originalFileName,
            String contentType,
            byte[] bytes,
            String expectedExtension,
            String expectedContentType
    ) {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                originalFileName,
                contentType,
                bytes
        );

        StoredAiChatImage stored = storage.store(image);

        assertThat(stored.storedFileName()).matches(
                "^[0-9a-f-]{36}\\." + expectedExtension + "$"
        );
        assertThat(stored.imageUrl()).isEqualTo(
                "/api/ai-chats/images/" + stored.storedFileName()
        );
        verify(objectStorage).upload(
                eq("ai-chat-images/" + stored.storedFileName()),
                eq(expectedContentType),
                eq((long) bytes.length),
                any(InputStream.class)
        );
    }

    private byte[] jpegBytes() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
    }

    private byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };
    }

    private byte[] webpBytes() {
        return new byte[]{
                0x52, 0x49, 0x46, 0x46,
                0x00, 0x00, 0x00, 0x00,
                0x57, 0x45, 0x42, 0x50
        };
    }
}
