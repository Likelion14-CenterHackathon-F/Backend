package com.centerton.centerton.domain.preconsultationsubmission.converter;

import com.centerton.centerton.domain.preconsultationsubmission.entity.enums.SymptomCategory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class SymptomCategoryConverter
        implements Converter<String, SymptomCategory> {

    @Override
    public SymptomCategory convert(String source) {
        return SymptomCategory.fromValue(source);
    }
}
