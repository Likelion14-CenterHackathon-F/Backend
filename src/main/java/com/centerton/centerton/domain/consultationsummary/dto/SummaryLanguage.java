package com.centerton.centerton.domain.consultationsummary.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum SummaryLanguage {
    KO("KO", "KO", "KOREAN", "한국어"),
    EN("EN-US", "EN", "EN-US", "ENGLISH", "영어"),
    JA("JA", "JA", "JP", "JAPANESE", "일본어"),
    ZH("ZH-HANS", "ZH", "ZH-HANS", "CHINESE", "중국어");

    private final String deepLTargetCode;
    private final String[] aliases;

    SummaryLanguage(String deepLTargetCode, String... aliases) {
        this.deepLTargetCode = deepLTargetCode;
        this.aliases = aliases;
    }

    public boolean isKorean() {
        return this == KO;
    }

    public String getDeepLTargetCode() {
        return deepLTargetCode;
    }

    @JsonValue
    public String getResponseCode() {
        return deepLTargetCode;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static SummaryLanguage from(String value) {
        if (value == null || value.isBlank()) {
            return KO;
        }

        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(language -> language.name().equalsIgnoreCase(normalized)
                        || language.deepLTargetCode.equalsIgnoreCase(normalized)
                        || Arrays.stream(language.aliases)
                        .anyMatch(alias -> alias.equalsIgnoreCase(normalized)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 상담 요약 언어입니다: " + value
                ));
    }
}
