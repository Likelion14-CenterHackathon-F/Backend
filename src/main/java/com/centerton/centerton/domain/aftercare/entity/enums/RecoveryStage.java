package com.centerton.centerton.domain.aftercare.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecoveryStage {

    EARLY("회복 초기 단계"),
    MIDDLE("회복 중기 단계"),
    STABLE("회복 안정 단계");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static RecoveryStage fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = value.trim();
        for (RecoveryStage stage : values()) {
            if (stage.name().equalsIgnoreCase(normalizedValue) || stage.getValue().equals(normalizedValue)) {
                return stage;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 회복 단계입니다: " + value);
    }
}
