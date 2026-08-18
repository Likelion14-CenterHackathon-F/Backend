package com.centerton.centerton.domain.preconsultationsubmission.storage;

import com.centerton.centerton.domain.preconsultationsubmission.exception.PreconsultSubmissionErrorCode;
import com.centerton.centerton.global.exception.BaseException;
import com.centerton.centerton.global.storage.s3.S3ObjectStorage;
import com.centerton.centerton.global.storage.s3.S3StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3PreconsultFileStorageTest {

    @Mock
    private S3ObjectStorage objectStorage;

    private S3PreconsultFileStorage storage;

    @BeforeEach
    void setUp() {
        storage = new S3PreconsultFileStorage(
                objectStorage,
                new PreconsultFileValidator()
        );
    }

    @Test
    void storesJpegWithDomainPrefixAndUuidFileName() {
        assertStoredFile(
                "photo.jpeg",
                "image/jpeg",
                jpegBytes(),
                "jpeg",
                "image/jpeg"
        );
    }

    @Test
    void storesPngWithDomainPrefixAndUuidFileName() {
        assertStoredFile(
                "photo.png",
                "image/png",
                pngBytes(),
                "png",
                "image/png"
        );
    }

    @Test
    void storesMp4WithDomainPrefixAndUuidFileName() {
        assertStoredFile(
                "video.mp4",
                "video/mp4",
                mp4Bytes(),
                "mp4",
                "video/mp4"
        );
    }

    @Test
    void mapsMissingS3ObjectToExistingFileNotFoundError() {
        when(objectStorage.load("preconsult-submissions/missing.jpg"))
                .thenThrow(S3StorageException.objectNotFound(
                        new IllegalStateException()
                ));

        assertThatThrownBy(() -> storage.load("missing.jpg"))
                .isInstanceOfSatisfying(
                        BaseException.class,
                        exception -> assertThat(exception.getBaseResponseCode())
                                .isEqualTo(
                                        PreconsultSubmissionErrorCode.FILE_NOT_FOUND
                                )
                );
    }

    @Test
    void deletesUsingTheSameDomainObjectKey() {
        storage.delete("stored.png");

        verify(objectStorage).delete(
                "preconsult-submissions/stored.png"
        );
    }

    private void assertStoredFile(
            String originalFileName,
            String contentType,
            byte[] bytes,
            String expectedExtension,
            String expectedContentType
    ) {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                originalFileName,
                contentType,
                bytes
        );

        StoredPreconsultFile stored = storage.store(file);

        assertThat(stored.storedFileName()).matches(
                "^[0-9a-f-]{36}\\." + expectedExtension + "$"
        );
        assertThat(stored.contentType()).isEqualTo(expectedContentType);
        verify(objectStorage).upload(
                eq("preconsult-submissions/" + stored.storedFileName()),
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

    private byte[] mp4Bytes() {
        return new byte[]{
                0x00, 0x00, 0x00, 0x18,
                0x66, 0x74, 0x79, 0x70,
                0x69, 0x73, 0x6F, 0x6D
        };
    }
}
