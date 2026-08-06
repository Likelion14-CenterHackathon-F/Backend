package com.centerton.centerton.domain.consultationsummary.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({GeminiProperties.class, DeepLProperties.class})
public class ConsultationSummaryAiConfig {

    @Bean
    public RestClient geminiRestClient(
            RestClient.Builder builder,
            GeminiProperties properties
    ) {
        return builder.baseUrl(properties.getBaseUrl()).build();
    }

    @Bean
    public RestClient deepLRestClient(
            RestClient.Builder builder,
            DeepLProperties properties
    ) {
        return builder.baseUrl(properties.getBaseUrl()).build();
    }
}
