package com.centerton.centerton.domain.aichat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {

    private String apiKey;
    private String baseUrl = "https://api.openai.com";
    private String model = "gpt-5.6-luna";
    private int maxOutputTokens = 900;
}
