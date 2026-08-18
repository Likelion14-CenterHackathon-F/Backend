package com.centerton.centerton.global.storage.s3;

import org.springframework.core.io.AbstractResource;

import java.io.IOException;
import java.io.InputStream;

public class S3Resource extends AbstractResource {

    private final S3ObjectStorage objectStorage;
    private final String objectKey;
    private final long contentLength;

    public S3Resource(
            S3ObjectStorage objectStorage,
            String objectKey,
            long contentLength
    ) {
        this.objectStorage = objectStorage;
        this.objectKey = objectKey;
        this.contentLength = contentLength;
    }

    @Override
    public String getDescription() {
        return "S3 object [" + objectKey + "]";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            return objectStorage.openStream(objectKey);
        } catch (S3StorageException exception) {
            throw new IOException("Failed to open S3 object stream", exception);
        }
    }

    @Override
    public long contentLength() {
        return contentLength;
    }

    @Override
    public String getFilename() {
        int separatorIndex = objectKey.lastIndexOf('/');
        return objectKey.substring(separatorIndex + 1);
    }
}
