package com.centerton.centerton.domain.aichat.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ChatMessageRole {
    USER("사용자"),
    ASSISTANT("AI");

    private final String value;

    ChatMessageRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ChatMessageRole from(String value) {
        if (value == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(role -> role.value.equalsIgnoreCase(value)
                        || role.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 채팅 메시지 역할입니다: " + value
                ));
    }
}
