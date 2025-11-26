package com.app.civify.converter;

import com.app.civify.dto.resume.ResumeData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Converter
@Component
@RequiredArgsConstructor
@Slf4j
public class ResumeDataConverter implements AttributeConverter<ResumeData, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ResumeData attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Error converting ResumeData to JSON string", e);
            throw new RuntimeException("Error converting ResumeData to JSON string", e);
        }
    }

    @Override
    public ResumeData convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new ResumeData();
        }
        try {
            return objectMapper.readValue(dbData, ResumeData.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON string to ResumeData", e);
            return new ResumeData(); // Return empty object on error to avoid crash
        }
    }
}
