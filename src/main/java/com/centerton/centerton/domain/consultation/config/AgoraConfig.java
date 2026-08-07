package com.centerton.centerton.domain.consultation.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AgoraProperties.class)
public class AgoraConfig {

    @Bean
    public RestClient agoraSttRestClient(
            RestClient.Builder builder,
            AgoraProperties agoraProperties
    ) {
        return builder
                .baseUrl(agoraProperties.getStt().getBaseUrl())
                .defaultHeaders(headers -> headers.setBasicAuth(
                        agoraProperties.getCustomerId(),
                        agoraProperties.getCustomerSecret()
                ))
                .build();
    }
}
