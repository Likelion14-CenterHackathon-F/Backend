package com.centerton.centerton.domain.consultationsummary.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    private String apiKey;
    private String baseUrl = "https://generativelanguage.googleapis.com";
    private String model = "gemini-3.6-flash";

    /*
     * gemini-3.6-flash 최대 output token limit.
     *
     * 상담 요약 JSON이 중간에 잘리는 문제를 방지하기 위해
     * 모델이 지원하는 최대 출력 토큰까지 허용합니다.
     */
    private int maxOutputTokens = 65_536;
}