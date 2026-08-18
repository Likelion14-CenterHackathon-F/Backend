package com.centerton.centerton.domain.aichat.storage;

import com.centerton.centerton.domain.aichat.exception.AiChatErrorCode;
import com.centerton.centerton.global.exception.BaseException;
import com.centerton.centerton.global.storage.s3.S3ObjectStorage;
import com.centerton.centerton.global.storage.s3.S3StorageException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        prefix = "app.storage",
        name = "provider",
        havingValue = "s3"
)
public class S3AiChatImageStorage implements AiChatImageStorage {

    private static final String OBJECT_KEY_PREFIX = "ai-chat-images/";

    private final S3ObjectStorage objectStorage;
    private final AiChatImageValidator imageValidator;

    public S3AiChatImageStorage(
            S3ObjectStorage objectStorage,
            AiChatImageValidator imageValidator
    ) {
        this.objectStorage = objectStorage;
        this.imageValidator = imageValidator;
    }

    @Override
    public StoredAiChatImage store(MultipartFile image) {
        ValidatedAiChatImage validatedImage = imageValidator.validate(image);
        String storedFileName = UUID.randomUUID()
                + "."
                + validatedImage.extension();

        try (InputStream inputStream = image.getInputStream()) {
            objectStorage.upload(
                    objectKey(storedFileName),
                    validatedImage.imageType().getResponseContentType(),
                    image.getSize(),
                    inputStream
            );
        } catch (IOException | S3StorageException exception) {
            throw new BaseException(AiChatErrorCode.IMAGE_STORAGE_FAILED);
        }

        return new StoredAiChatImage(
                storedFileName,
                resolveDisplayImageUrl(storedFileName)
        );
    }

    @Override
    public Resource load(String storedFileName) {
        try {
            return objectStorage.load(objectKey(storedFileName));
        } catch (S3StorageException exception) {
            throw mapStorageException(exception);
        }
    }

    @Override
    public String resolveDisplayImageUrl(String storedFileName) {
        return IMAGE_URL_PREFIX + storedFileName;
    }

    @Override
    public String resolveAnalysisImageUrl(String storedFileName) {
        try {
            return "data:"
                    + resolveContentType(storedFileName)
                    + ";base64,"
                    + Base64.getEncoder().encodeToString(
                            objectStorage.readAllBytes(objectKey(storedFileName))
                    );
        } catch (S3StorageException exception) {
            throw mapStorageException(exception);
        }
    }

    @Override
    public String resolveContentType(String storedFileName) {
        return imageValidator.resolveStoredImageType(storedFileName)
                .getResponseContentType();
    }

    @Override
    public void delete(String storedFileName) {
        try {
            objectStorage.delete(objectKey(storedFileName));
        } catch (S3StorageException exception) {
            throw new BaseException(AiChatErrorCode.IMAGE_STORAGE_FAILED);
        }
    }

    private String objectKey(String storedFileName) {
        return OBJECT_KEY_PREFIX + storedFileName;
    }

    private BaseException mapStorageException(S3StorageException exception) {
        if (exception.isObjectNotFound()) {
            return new BaseException(AiChatErrorCode.IMAGE_NOT_FOUND);
        }

        return new BaseException(AiChatErrorCode.IMAGE_STORAGE_FAILED);
    }
}
