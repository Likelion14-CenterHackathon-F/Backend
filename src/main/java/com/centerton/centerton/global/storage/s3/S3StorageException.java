package com.centerton.centerton.global.storage.s3;

public class S3StorageException extends RuntimeException {

    private final Reason reason;

    private S3StorageException(Reason reason, Throwable cause) {
        super(cause);
        this.reason = reason;
    }

    public static S3StorageException objectNotFound(Throwable cause) {
        return new S3StorageException(Reason.OBJECT_NOT_FOUND, cause);
    }

    public static S3StorageException operationFailed(Throwable cause) {
        return new S3StorageException(Reason.OPERATION_FAILED, cause);
    }

    public boolean isObjectNotFound() {
        return reason == Reason.OBJECT_NOT_FOUND;
    }

    private enum Reason {
        OBJECT_NOT_FOUND,
        OPERATION_FAILED
    }
}
