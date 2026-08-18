package com.centerton.centerton.domain.aichat.storage;

import com.centerton.centerton.domain.aichat.exception.AiChatErrorCode;
import com.centerton.centerton.global.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        prefix = "app.storage",
        name = "provider",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalAiChatImageStorage implements AiChatImageStorage {

    private final Path storageDirectory;
    private final AiChatImageValidator imageValidator;

    public LocalAiChatImageStorage(
            @Value("${ai-chat.image.storage.directory:uploads/ai-chat-images}")
            String storageDirectory,
            AiChatImageValidator imageValidator
    ) {
        this.storageDirectory = Path.of(storageDirectory)
                .toAbsolutePath()
                .normalize();
        this.imageValidator = imageValidator;
    }

    @Override
    public StoredAiChatImage store(MultipartFile image) {
        ValidatedAiChatImage validatedImage = imageValidator.validate(image);
        String storedFileName = UUID.randomUUID()
                + "."
                + validatedImage.extension();

        Path temporaryFile = null;

        try {
            Files.createDirectories(storageDirectory);
            temporaryFile = Files.createTempFile(
                    storageDirectory,
                    "ai-chat-image-",
                    ".tmp"
            );

            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(
                        inputStream,
                        temporaryFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            moveToFinalLocation(
                    temporaryFile,
                    resolveStoredPath(storedFileName)
            );

            return new StoredAiChatImage(
                    storedFileName,
                    resolveDisplayImageUrl(storedFileName)
            );
        } catch (IOException exception) {
            deleteTemporaryFile(temporaryFile);
            throw new BaseException(AiChatErrorCode.IMAGE_STORAGE_FAILED);
        }
    }

    @Override
    public Resource load(String storedFileName) {
        Path storedPath = resolveStoredPath(storedFileName);

        if (!Files.isRegularFile(storedPath)) {
            throw new BaseException(AiChatErrorCode.IMAGE_NOT_FOUND);
        }

        return new FileSystemResource(storedPath);
    }

    @Override
    public String resolveDisplayImageUrl(String storedFileName) {
        return IMAGE_URL_PREFIX + storedFileName;
    }

    @Override
    public String resolveAnalysisImageUrl(String storedFileName) {
        try {
            byte[] bytes = Files.readAllBytes(resolveStoredPath(storedFileName));
            return "data:"
                    + resolveContentType(storedFileName)
                    + ";base64,"
                    + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException exception) {
            throw new BaseException(AiChatErrorCode.IMAGE_NOT_FOUND);
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
            Files.deleteIfExists(resolveStoredPath(storedFileName));
        } catch (IOException exception) {
            throw new BaseException(AiChatErrorCode.IMAGE_STORAGE_FAILED);
        }
    }

    private void moveToFinalLocation(
            Path temporaryFile,
            Path finalFile
    ) throws IOException {
        try {
            Files.move(
                    temporaryFile,
                    finalFile,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, finalFile);
        }
    }

    private Path resolveStoredPath(String storedFileName) {
        Path storedPath = storageDirectory.resolve(storedFileName).normalize();

        if (!storedPath.startsWith(storageDirectory)) {
            throw new BaseException(AiChatErrorCode.IMAGE_NOT_FOUND);
        }

        return storedPath;
    }

    private void deleteTemporaryFile(Path temporaryFile) {
        if (temporaryFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException ignored) {
            // 원래 발생한 파일 저장 예외를 유지합니다.
        }
    }
}
