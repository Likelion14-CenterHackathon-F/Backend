package com.centerton.centerton.global.storage;

import com.centerton.centerton.domain.aichat.storage.AiChatImageStorage;
import com.centerton.centerton.domain.aichat.storage.AiChatImageValidator;
import com.centerton.centerton.domain.aichat.storage.LocalAiChatImageStorage;
import com.centerton.centerton.domain.aichat.storage.S3AiChatImageStorage;
import com.centerton.centerton.domain.preconsultationsubmission.storage.LocalPreconsultFileStorage;
import com.centerton.centerton.domain.preconsultationsubmission.storage.PreconsultFileStorage;
import com.centerton.centerton.domain.preconsultationsubmission.storage.PreconsultFileValidator;
import com.centerton.centerton.domain.preconsultationsubmission.storage.S3PreconsultFileStorage;
import com.centerton.centerton.global.storage.s3.S3ObjectStorage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StorageProviderConditionTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(StorageTestConfig.class);

    @Test
    void missingProviderRegistersOnlyLocalImplementations() {
        assertLocalStorage(contextRunner);
    }

    @Test
    void localProviderRegistersOnlyLocalImplementations() {
        assertLocalStorage(contextRunner.withPropertyValues(
                "app.storage.provider=local"
        ));
    }

    @Test
    void s3ProviderRegistersOnlyS3Implementations() {
        contextRunner.withPropertyValues("app.storage.provider=s3")
                .run(context -> {
                    assertThat(context).hasSingleBean(PreconsultFileStorage.class);
                    assertThat(context).hasSingleBean(AiChatImageStorage.class);
                    assertThat(context.getBean(PreconsultFileStorage.class))
                            .isInstanceOf(S3PreconsultFileStorage.class);
                    assertThat(context.getBean(AiChatImageStorage.class))
                            .isInstanceOf(S3AiChatImageStorage.class);
                });
    }

    private void assertLocalStorage(ApplicationContextRunner runner) {
        runner.run(context -> {
            assertThat(context).hasSingleBean(PreconsultFileStorage.class);
            assertThat(context).hasSingleBean(AiChatImageStorage.class);
            assertThat(context.getBean(PreconsultFileStorage.class))
                    .isInstanceOf(LocalPreconsultFileStorage.class);
            assertThat(context.getBean(AiChatImageStorage.class))
                    .isInstanceOf(LocalAiChatImageStorage.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            PreconsultFileValidator.class,
            LocalPreconsultFileStorage.class,
            S3PreconsultFileStorage.class,
            AiChatImageValidator.class,
            LocalAiChatImageStorage.class,
            S3AiChatImageStorage.class
    })
    static class StorageTestConfig {

        @Bean
        S3ObjectStorage s3ObjectStorage() {
            return mock(S3ObjectStorage.class);
        }
    }
}
