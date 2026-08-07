package com.centerton.centerton.domain.consultation.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ParticipantRole {
    PATIENT("환자"),
    MEDICAL_STAFF("의료진");

    private final String value;

    ParticipantRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ParticipantRole from(String value) {
        if (value == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(role -> role.value.equalsIgnoreCase(value)
                        || role.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 상담 참여자 역할입니다: " + value
                ));
    }
}
