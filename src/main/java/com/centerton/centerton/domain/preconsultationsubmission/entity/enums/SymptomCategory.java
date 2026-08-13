package com.centerton.centerton.domain.preconsultationsubmission.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SymptomCategory {

    PAIN("통증"),
    SWELLING("붓기"),
    REDNESS("홍조"),
    HEAT("열감"),
    BLEEDING("출혈"),
    ITCHING("가려움"),
    BRUISING("멍"),
    OTHER("기타");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static SymptomCategory fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = value.trim();
        for (SymptomCategory category : values()) {
            if (category.name().equalsIgnoreCase(normalizedValue)
                    || category.getValue().equals(normalizedValue)) {
                return category;
            }
        }
        throw new IllegalArgumentException(
                "지원하지 않는 증상 분류입니다: " + value
        );
    }
}
