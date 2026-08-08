package com.centerton.centerton.domain.preconsultationsubmission.storage;

import com.centerton.centerton.domain.preconsultationsubmission.exception.PreconsultSubmissionErrorCode;
import com.centerton.centerton.global.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.UUID;

@Component
public class LocalPreconsultFileStorage implements PreconsultFileStorage {

    private final Path storageDirectory;
    private final PreconsultFileValidator fileValidator;

    public LocalPreconsultFileStorage(
            @Value("${preconsult-submission.storage.directory:uploads/preconsult-submissions}")
            String storageDirectory,
            PreconsultFileValidator fileValidator
    ) {
        this.storageDirectory = Path.of(storageDirectory)
                .toAbsolutePath()
                .normalize();
        this.fileValidator = fileValidator;
    }

    @Override
    public StoredPreconsultFile store(MultipartFile file) {
        ValidatedPreconsultFile validatedFile = fileValidator.validate(file);
        String storedFileName = UUID.randomUUID()
                + "."
                + validatedFile.extension();

        Path temporaryFile = null;

        try {
            Files.createDirectories(storageDirectory);
            temporaryFile = Files.createTempFile(
                    storageDirectory,
                    "upload-",
                    ".tmp"
            );

            try (InputStream inputStream = file.getInputStream()) {
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

            return new StoredPreconsultFile(
                    storedFileName,
                    validatedFile.fileType().getResponseContentType()
            );
        } catch (IOException exception) {
            deleteTemporaryFile(temporaryFile);
            throw new BaseException(
                    PreconsultSubmissionErrorCode.FILE_STORAGE_FAILED
            );
        }
    }

    @Override
    public Resource load(String storedFileName) {
        Path storedPath = resolveStoredPath(storedFileName);

        if (!Files.isRegularFile(storedPath)) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.FILE_NOT_FOUND
            );
        }

        return new FileSystemResource(storedPath);
    }

    @Override
    public void delete(String storedFileName) {
        try {
            Files.deleteIfExists(resolveStoredPath(storedFileName));
        } catch (IOException exception) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.FILE_STORAGE_FAILED
            );
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
            throw new BaseException(
                    PreconsultSubmissionErrorCode.FILE_NOT_FOUND
            );
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
