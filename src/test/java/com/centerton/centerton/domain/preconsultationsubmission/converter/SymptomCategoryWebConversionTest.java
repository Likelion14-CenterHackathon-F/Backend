package com.centerton.centerton.domain.preconsultationsubmission.converter;

import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SymptomCategoryWebConversionTest {

    @Autowired
    private WebApplicationContext context;

    @Test
    void mvcConversionServiceAcceptsKoreanMultipartFormValue() {
        ConversionService conversionService = context.getBean(
                "mvcConversionService",
                ConversionService.class
        );

        assertThat(conversionService.convert(
                "열감",
                SymptomCategory.class
        )).isEqualTo(SymptomCategory.HEAT);
        assertThat(conversionService.convert(
                "BLEEDING",
                SymptomCategory.class
        )).isEqualTo(SymptomCategory.BLEEDING);
    }
}
