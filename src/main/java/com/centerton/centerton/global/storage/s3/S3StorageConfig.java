package com.centerton.centerton.global.storage.s3;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.storage",
        name = "provider",
        havingValue = "s3"
)
@EnableConfigurationProperties(S3StorageProperties.class)
public class S3StorageConfig {

    @Bean
    public S3Client s3Client(S3StorageProperties properties) {
        validateProperties(properties);

        return S3Client.builder()
                .region(Region.of(properties.region().strip()))
                .build();
    }

    private void validateProperties(S3StorageProperties properties) {
        if (properties.region() == null || properties.region().isBlank()) {
            throw new IllegalStateException(
                    "app.storage.s3.region must be configured for S3 storage"
            );
        }

        if (properties.bucket() == null || properties.bucket().isBlank()) {
            throw new IllegalStateException(
                    "app.storage.s3.bucket must be configured for S3 storage"
            );
        }
    }
}
