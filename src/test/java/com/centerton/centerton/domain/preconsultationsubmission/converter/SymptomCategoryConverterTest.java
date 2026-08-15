package com.centerton.centerton.domain.preconsultationsubmission.converter;

import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SymptomCategoryConverterTest {

    private final SymptomCategoryConverter converter =
            new SymptomCategoryConverter();

    @Test
    void convertsKoreanMultipartFormValue() {
        assertThat(converter.convert("통증"))
                .isEqualTo(SymptomCategory.PAIN);
    }

    @Test
    void convertsEnumNameMultipartFormValueIgnoringCase() {
        assertThat(converter.convert("bruising"))
                .isEqualTo(SymptomCategory.BRUISING);
    }
}
