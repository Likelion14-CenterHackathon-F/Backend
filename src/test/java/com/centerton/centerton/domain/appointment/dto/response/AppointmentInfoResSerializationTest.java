package com.centerton.centerton.domain.appointment.dto.response;

import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentInfoResSerializationTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void symptomCategoriesAreSerializedUsingKoreanJsonValues() {
        AppointmentInfoRes response = new AppointmentInfoRes(
                101L,
                OffsetDateTime.parse("2026-07-30T14:00:00Z"),
                OffsetDateTime.parse("2026-07-30T14:15:00Z"),
                List.of(
                        SymptomCategory.SWELLING,
                        SymptomCategory.BRUISING
                ),
                "수술 부위가 붓고 멍이 심합니다."
        );

        String json = jsonMapper.writeValueAsString(response);

        assertThat(json).contains(
                "\"symptomCategories\":[\"붓기\",\"멍\"]"
        );
        assertThat(json).doesNotContain("SWELLING", "BRUISING");
    }
}
