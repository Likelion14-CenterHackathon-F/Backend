package com.centerton.centerton.global.storage.s3;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;

@Component
@ConditionalOnProperty(
        prefix = "app.storage",
        name = "provider",
        havingValue = "s3"
)
public class S3ObjectStorage {

    private final S3Client s3Client;
    private final String bucket;

    public S3ObjectStorage(
            S3Client s3Client,
            S3StorageProperties properties
    ) {
        this.s3Client = s3Client;
        this.bucket = properties.bucket().strip();
    }

    public void upload(
            String objectKey,
            String contentType,
            long contentLength,
            InputStream inputStream
    ) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        try {
            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(inputStream, contentLength)
            );
        } catch (SdkException exception) {
            throw S3StorageException.operationFailed(exception);
        }
    }

    public Resource load(String objectKey) {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        try {
            HeadObjectResponse response = s3Client.headObject(request);
            return new S3Resource(this, objectKey, response.contentLength());
        } catch (SdkException exception) {
            throw mapException(exception);
        }
    }

    public byte[] readAllBytes(String objectKey) {
        try (InputStream inputStream = openStream(objectKey)) {
            return inputStream.readAllBytes();
        } catch (S3StorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw S3StorageException.operationFailed(exception);
        }
    }

    public void delete(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (SdkException exception) {
            throw S3StorageException.operationFailed(exception);
        }
    }

    ResponseInputStream<GetObjectResponse> openStream(String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        try {
            return s3Client.getObject(request);
        } catch (SdkException exception) {
            throw mapException(exception);
        }
    }

    private S3StorageException mapException(SdkException exception) {
        if (exception instanceof NoSuchKeyException
                || exception instanceof S3Exception s3Exception
                && s3Exception.statusCode() == 404) {
            return S3StorageException.objectNotFound(exception);
        }

        return S3StorageException.operationFailed(exception);
    }
}
