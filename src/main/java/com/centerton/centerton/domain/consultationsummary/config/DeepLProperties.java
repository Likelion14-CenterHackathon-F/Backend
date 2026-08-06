package com.centerton.centerton.domain.consultationsummary.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "deepl")
public class DeepLProperties {

    private String authKey;
    private String baseUrl = "https://api-free.deepl.com";
}
