package com.centerton.centerton.domain.preconsultationsubmission.storage;

import com.centerton.centerton.domain.preconsultationsubmission.exception.PreconsultSubmissionErrorCode;
import com.centerton.centerton.global.exception.BaseException;
import com.centerton.centerton.global.storage.s3.S3ObjectStorage;
import com.centerton.centerton.global.storage.s3.S3StorageException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        prefix = "app.storage",
        name = "provider",
        havingValue = "s3"
)
public class S3PreconsultFileStorage implements PreconsultFileStorage {

    private static final String OBJECT_KEY_PREFIX = "preconsult-submissions/";

    private final S3ObjectStorage objectStorage;
    private final PreconsultFileValidator fileValidator;

    public S3PreconsultFileStorage(
            S3ObjectStorage objectStorage,
            PreconsultFileValidator fileValidator
    ) {
        this.objectStorage = objectStorage;
        this.fileValidator = fileValidator;
    }

    @Override
    public StoredPreconsultFile store(MultipartFile file) {
        ValidatedPreconsultFile validatedFile = fileValidator.validate(file);
        String storedFileName = UUID.randomUUID()
                + "."
                + validatedFile.extension();

        try (InputStream inputStream = file.getInputStream()) {
            objectStorage.upload(
                    objectKey(storedFileName),
                    validatedFile.fileType().getResponseContentType(),
                    file.getSize(),
                    inputStream
            );
        } catch (IOException | S3StorageException exception) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.FILE_STORAGE_FAILED
            );
        }

        return new StoredPreconsultFile(
                storedFileName,
                validatedFile.fileType().getResponseContentType()
        );
    }

    @Override
    public Resource load(String storedFileName) {
        try {
            return objectStorage.load(objectKey(storedFileName));
        } catch (S3StorageException exception) {
            if (exception.isObjectNotFound()) {
                throw new BaseException(
                        PreconsultSubmissionErrorCode.FILE_NOT_FOUND
                );
            }

            throw new BaseException(
                    PreconsultSubmissionErrorCode.FILE_STORAGE_FAILED
            );
        }
    }

    @Override
    public void delete(String storedFileName) {
        try {
            objectStorage.delete(objectKey(storedFileName));
        } catch (S3StorageException exception) {
            throw new BaseException(
                    PreconsultSubmissionErrorCode.FILE_STORAGE_FAILED
            );
        }
    }

    private String objectKey(String storedFileName) {
        return OBJECT_KEY_PREFIX + storedFileName;
    }
}
