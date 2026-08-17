package com.centerton.centerton.domain.aichat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai-chat.rag-service")
public class AiRagServiceProperties {

    private String baseUrl = "";
    private String path = "/v1/aftercare/answer";
}
