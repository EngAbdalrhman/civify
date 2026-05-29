package com.app.civify.service.cv;

import com.app.civify.dto.cv.AtsFormatReport;
import com.app.civify.dto.cv.CvTailorResponse.AtsScore;
import com.app.civify.dto.resume.ResumeData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AtsCompatibilityServiceTest {

    private AtsCompatibilityService service;

    @BeforeEach
    void setUp() {
        service = new AtsCompatibilityService();
    }

    @Test
    void analyze_atsTemplate_scoresHigh() {
        ResumeData data = baseResume();
        data.getMetadata().setTemplateId("ats");
        data.getMetadata().setSkillsDisplayStyle("tags");

        AtsFormatReport report = service.analyze(data);

        assertTrue(report.getFormatScore() >= 85);
        assertEquals("excellent", report.getAtsReadiness());
    }

    @Test
    void analyze_creativeTemplate_scoresLowerWithIssues() {
        ResumeData data = baseResume();
        data.getMetadata().setTemplateId("creative");
        data.getMetadata().setSkillsDisplayStyle("bar");

        AtsFormatReport report = service.analyze(data);

        assertTrue(report.getFormatScore() < 70);
        assertFalse(report.getIssues().isEmpty());
        assertEquals("ats", report.getRecommendedTemplateId());
    }

    @Test
    void enrichScore_blendsContentAndFormat() {
        ResumeData data = baseResume();
        data.getMetadata().setTemplateId("creative");

        AtsScore raw = AtsScore.builder()
                .overall(80)
                .keywordMatch(75)
                .skillsMatch(70)
                .build();

        AtsScore enriched = service.enrichScore(raw, data);

        assertNotNull(enriched.getFormatScore());
        assertNotNull(enriched.getContentScore());
        assertTrue(enriched.getOverall() <= 80);
        assertNotNull(enriched.getAtsReadiness());
    }

    private static ResumeData baseResume() {
        ResumeData data = new ResumeData();
        ResumeData.PersonalInfo pi = new ResumeData.PersonalInfo();
        pi.setFullName("Jane Doe");
        pi.setEmail("jane@example.com");
        pi.setPhone("+1234567890");
        pi.setSummary("Experienced engineer.");
        data.setPersonalInfo(pi);

        ResumeData.ResumeMetadata meta = new ResumeData.ResumeMetadata();
        meta.setTemplateId("classic");
        data.setMetadata(meta);

        ResumeData.ResumeSection exp = new ResumeData.ResumeSection();
        exp.setType("experience");
        ResumeData.SectionItem item = new ResumeData.SectionItem();
        item.setTitle("Acme Corp");
        item.setSubtitle("Engineer");
        item.setDescription("Built APIs serving 1M users.");
        exp.setItems(java.util.List.of(item));
        data.setSections(java.util.List.of(exp));
        return data;
    }
}
