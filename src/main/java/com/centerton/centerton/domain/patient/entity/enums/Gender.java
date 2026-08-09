package com.centerton.centerton.domain.patient.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Gender {

    MALE("남성", "Male"),
    FEMALE("여성", "Female"),
    OTHER("기타", "Other"),
    UNKNOWN("알 수 없음", "Unknown");

    private final String value;
    private final String englishValue;

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return value + " (" + englishValue + ")";
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Gender fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = value.trim();
        for (Gender gender : values()) {
            if (gender.name().equalsIgnoreCase(normalizedValue) || gender.getValue().equals(normalizedValue)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 성별입니다: " + value);
    }
}
