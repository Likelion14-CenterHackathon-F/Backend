package com.centerton.centerton.domain.consultation.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ConsultationLanguage {

    KOREAN(
            "ko-KR",
            "KO",
            "KOREAN",
            "한국어"
    ),

    CHINESE(
            "zh-CN",
            "ZH",
            "ZH-HANS",
            "CHINESE",
            "중국어"
    ),

    JAPANESE(
            "ja-JP",
            "JA",
            "JP",
            "JAPANESE",
            "일본어"
    ),

    ENGLISH(
            "en-US",
            "EN",
            "ENGLISH",
            "영어"
    );

    private final String agoraCode;
    private final String[] aliases;

    ConsultationLanguage(
            String agoraCode,
            String... aliases
    ) {
        this.agoraCode = agoraCode;
        this.aliases = aliases;
    }

    @JsonValue
    public String getAgoraCode() {
        return agoraCode;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ConsultationLanguage from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "상담 언어는 필수입니다."
            );
        }

        String normalized = value.trim();

        return Arrays.stream(values())
                .filter(language ->
                        language.name().equalsIgnoreCase(normalized)
                                || language.agoraCode.equalsIgnoreCase(normalized)
                                || Arrays.stream(language.aliases)
                                .anyMatch(alias ->
                                        alias.equalsIgnoreCase(normalized)
                                )
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "지원하지 않는 상담 언어입니다: " + value
                        )
                );
    }
}