package com.app.civify.dto.resume;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResumeData implements Serializable {
    private PersonalInfo personalInfo = new PersonalInfo();
    private List<ResumeSection> sections = new ArrayList<>();
    private ResumeMetadata metadata = new ResumeMetadata();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PersonalInfo implements Serializable {
        private String fullName;
        private String email;
        private String phone;
        private String address;
        private String city;
        private String country;
        private String summary;
        private String jobTitle;
        private String linkedin;
        private String website;
        private String github;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResumeSection implements Serializable {
        private String id;
        private String title;
        private String type; // e.g., "experience", "education", "skills", "custom"
        private List<SectionItem> items = new ArrayList<>();
        private boolean isVisible = true;
        private int order;
        private String style;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SectionItem implements Serializable {
        private String id;
        private String title; // e.g., Company Name or School Name
        private String subtitle; // e.g., Job Title or Degree
        private String date; // e.g., "2020 - Present"
        private String description; // HTML or Markdown content
        private String location;
        @JsonDeserialize(using = StringListDeserializer.class)
        private List<String> tags = new ArrayList<>(); // For skills or keywords
        private boolean isVisible = true;
    }

    public static class StringListDeserializer extends JsonDeserializer<List<String>> {
        @Override
        public List<String> deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            List<String> result = new ArrayList<>();
            // Check if current token is start of array
            if (p.isExpectedStartArrayToken()) {
                 JsonToken token;
                 while ((token = p.nextToken()) != JsonToken.END_ARRAY) {
                     if (token == JsonToken.VALUE_STRING) {
                         result.add(p.getText());
                     } else if (token == JsonToken.START_OBJECT) {
                         // Convert object to string representation - e.g. "Key: Value"
                         // This is a simple fallback to avoid crash
                         JsonNode node = p.getCodec().readTree(p);
                         result.add(node.toString()); 
                     } else {
                         // Try to get text value of any other primitive (number, boolean)
                         String val = p.getValueAsString();
                         if (val != null) result.add(val);
                     }
                 }
            } else {
                // Single value or object?
                // If it's a single string, wrap in list
                if (p.hasToken(JsonToken.VALUE_STRING)) {
                    result.add(p.getText());
                } else {
                    // unexpected token, return empty list or throw? Returning empty is safer for AI response
                    // Actually, if it's completely wrong type, maybe we consume and ignore or use toString
                     JsonNode node = p.getCodec().readTree(p);
                     if (node != null && !node.isNull()) {
                         result.add(node.toString());
                     }
                }
            }
            return result;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResumeMetadata implements Serializable {
        private String templateId = "modern";
        private String primaryColor = "#000000";
        private String fontFamily = "Inter";
        private String language = "en";
        private Map<String, String> customStyles;
    }
}
