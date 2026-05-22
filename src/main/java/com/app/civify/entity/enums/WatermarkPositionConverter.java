package com.app.civify.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class WatermarkPositionConverter implements AttributeConverter<WatermarkPosition, String> {

    @Override
    public String convertToDatabaseColumn(WatermarkPosition attribute) {
        return attribute == null ? WatermarkPosition.CENTER.getValue() : attribute.getValue();
    }

    @Override
    public WatermarkPosition convertToEntityAttribute(String dbData) {
        return WatermarkPosition.fromValue(dbData);
    }
}
