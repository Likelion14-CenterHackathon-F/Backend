package com.centerton.centerton.global.storage.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.s3")
public record S3StorageProperties(
        String region,
        String bucket
) {
}
