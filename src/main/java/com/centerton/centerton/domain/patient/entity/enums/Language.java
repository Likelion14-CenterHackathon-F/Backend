package com.centerton.centerton.domain.patient.entity.enums;

import com.centerton.centerton.domain.patient.exception.LanguageInvalidException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Language {

    KO("한국어"),
    JA("일본어"),
    EN("영어"),
    ZH("중국어");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Language fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (Language language : values()) {
            if (language.getValue().equals(value)) {
                return language;
            }
        }
        throw new LanguageInvalidException();
    }
}
