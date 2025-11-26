package com.app.civify.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
    public static class ResumeSection implements Serializable {
        private String id;
        private String title;
        private String type; // e.g., "experience", "education", "skills", "custom"
        private List<SectionItem> items = new ArrayList<>();
        private boolean isVisible = true;
        private int order;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SectionItem implements Serializable {
        private String id;
        private String title; // e.g., Company Name or School Name
        private String subtitle; // e.g., Job Title or Degree
        private String date; // e.g., "2020 - Present"
        private String description; // HTML or Markdown content
        private String location;
        private List<String> tags = new ArrayList<>(); // For skills or keywords
        private boolean isVisible = true;
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
