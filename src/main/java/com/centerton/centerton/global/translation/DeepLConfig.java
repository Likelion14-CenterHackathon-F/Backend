package com.centerton.centerton.global.translation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(DeepLProperties.class)
public class DeepLConfig {

    @Bean
    public RestClient deepLRestClient(
            RestClient.Builder builder,
            DeepLProperties properties
    ) {
        return builder.baseUrl(properties.getBaseUrl()).build();
    }
}
