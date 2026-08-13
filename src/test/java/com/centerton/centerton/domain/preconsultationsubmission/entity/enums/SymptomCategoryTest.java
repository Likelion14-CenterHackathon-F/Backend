package com.centerton.centerton.domain.preconsultationsubmission.entity.enums;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SymptomCategoryTest {

    @Test
    void exposesAllEightPlannedSymptomCategories() {
        assertThat(SymptomCategory.values())
                .extracting(SymptomCategory::getValue)
                .containsExactly(
                        "통증",
                        "붓기",
                        "홍조",
                        "열감",
                        "출혈",
                        "가려움",
                        "멍",
                        "기타"
                );
    }

    @Test
    void acceptsBothKoreanLabelsAndEnumNames() {
        Map<String, SymptomCategory> categories = Map.of(
                "통증", SymptomCategory.PAIN,
                "SWELLING", SymptomCategory.SWELLING,
                "홍조", SymptomCategory.REDNESS,
                "heat", SymptomCategory.HEAT,
                "출혈", SymptomCategory.BLEEDING,
                "ITCHING", SymptomCategory.ITCHING,
                "멍", SymptomCategory.BRUISING,
                "OTHER", SymptomCategory.OTHER
        );

        categories.forEach((value, expected) ->
                assertThat(SymptomCategory.fromValue(value))
                        .isEqualTo(expected)
        );
    }

    @Test
    void exposesKoreanLabelAndConvertsFromKoreanLabel() {
        assertThat(SymptomCategory.PAIN.getValue()).isEqualTo("통증");
        assertThat(SymptomCategory.fromValue("가려움"))
                .isEqualTo(SymptomCategory.ITCHING);
    }

    @Test
    void rejectsUnsupportedCategory() {
        assertThatThrownBy(() -> SymptomCategory.fromValue("발진"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 증상 분류");
    }
}
