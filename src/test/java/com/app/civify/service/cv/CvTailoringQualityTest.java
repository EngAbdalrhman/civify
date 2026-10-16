package com.app.civify.service.cv;

import com.app.civify.dto.ai.AiModelResponse;
import com.app.civify.dto.cv.CvTailorRequest;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.service.ai.AiService;
import com.app.civify.service.ai.JsonSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CvTailoringQualityTest {
    final AiService ai = mock(AiService.class);
    final KeywordMatcher keywords = mock(KeywordMatcher.class);
    final JsonSanitizer sanitizer = mock(JsonSanitizer.class);
    final CvEnhancementService service = new CvEnhancementService(ai, new ObjectMapper(), keywords,
            mock(ContentCleaner.class), mock(ResumeDiffService.class), sanitizer, new AtsCompatibilityService());
    @AfterEach void close() { service.shutdown(); }

    @Test void renamedAndMovedItemsAreNotRestoredAsDuplicates() {
        var original = resume(section("one", "projects", item("a", "Project Alpha", "Company", "2021")),
                section("two", "projects", item("b", "Project Beta", "Company", "2022"), item("c", "Unchanged", "Company", "2023")),
                section("edu", "education", item("d", "Computer Science", "University", "2020")));
        var tailored = resume(section("one", "projects", item("a", "Project Alpha (Company)", "", "2021"), item("b", "Project Beta (Company)", "", "2022")),
                section("edu", "education", item("d", "BSc Computer Science", "University", "2020")));
        ReflectionTestUtils.invokeMethod(service, "restoreLostSectionItems", original, tailored);
        assertEquals(4, tailored.getSections().stream().mapToInt(s -> s.getItems().size()).sum());
        assertEquals(1, tailored.getSections().stream().flatMap(s -> s.getItems().stream()).filter(i -> "b".equals(i.getId())).count());
        assertEquals(2, original.getSections().get(1).getItems().size(), "source stays unchanged");
    }

    @Test void sameJobTitleAtDifferentEmployersDoesNotHideLostExperience() {
        var original = resume(section("work", "experience", item(null, "Consultant", "One", "2021"), item(null, "Consultant", "Two", "2022")));
        var tailored = resume(section("work", "experience", item(null, "Consultant", "One", "2021")));
        ReflectionTestUtils.invokeMethod(service, "restoreLostSectionItems", original, tailored);
        assertEquals(2, tailored.getSections().getFirst().getItems().size());
    }

    @Test void alignmentChecksOrganizationsInsteadOfDegreeOrRoleTitles() {
        var original = resume(section("edu", "education", item("d", "Computer Science", "University", "2020")),
                section("work", "experience", item("a", "Developer", "Company", "2021")));
        var tailored = resume(section("edu", "education", item("d", "BSc Computer Science", "University", "2020")),
                section("work", "experience", item("a", "Software Developer", "Company", "2021")));
        List<String> warnings = ReflectionTestUtils.invokeMethod(service, "validateAlignment", original, tailored);
        assertTrue(warnings.isEmpty());
        tailored.getSections().getFirst().getItems().getFirst().setSubtitle("Invented University");
        warnings = ReflectionTestUtils.invokeMethod(service, "validateAlignment", original, tailored);
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("invented university"));
    }

    @Test void invalidAiScoreIsMarkedAsFallbackAndKeepsConsumedTokens() {
        when(keywords.match(anyString(), anyString())).thenReturn(new KeywordMatcher.MatchResult(40, List.of("OData")));
        when(ai.callModel(anyString(), any(), any())).thenReturn(AiModelResponse.builder().content("broken: [").totalTokens(123).build());
        when(sanitizer.cleanAiResponse(anyString())).thenReturn("broken: [");
        var score = service.scoreResumeForJob(resume(), "ABAP Consultant");
        assertEquals("KEYWORD_FALLBACK", score.getAnalysisSource());
        assertEquals(123, score.getTokensUsed());
        assertEquals(40, score.getKeywordMatch());
        assertTrue(score.getSuggestions().getFirst().contains("keyword matching only"));
        verify(ai, times(1)).callModel(anyString(), any(), any());
    }

    @Test void tailoringDoesNotInflateALowerMeasuredScore() {
        when(keywords.match(anyString(), anyString())).thenReturn(new KeywordMatcher.MatchResult(40, List.of()));
        when(keywords.extractKeywords(anyString())).thenReturn(Set.of());
        when(sanitizer.cleanAiResponse(anyString())).thenAnswer(call -> call.getArgument(0));
        ContentCleaner cleaner = (ContentCleaner) ReflectionTestUtils.getField(service, "contentCleaner");
        when(cleaner.clean(any(ResumeData.class))).thenAnswer(call -> call.getArgument(0));
        when(ai.callModel(anyString(), any(), any())).thenAnswer(call -> {
            String prompt = call.getArgument(0);
            String content = prompt.startsWith("You are an expert CV writer.")
                    ? "personalInfo:\n  fullName: Candidate\n  summary: TAILORED_MARKER\nsections: []"
                    : "overall: " + (prompt.contains("TAILORED_MARKER") ? "30" : "90")
                      + "\nkeywordMatch: 40\nskillsMatch: 50\nmissingKeywords: []\nsuggestions: []";
            return AiModelResponse.builder().content(content).totalTokens(10).build();
        });
        var result = service.tailorCv(CvTailorRequest.builder().resumeData(resume()).jobDescription("ABAP Consultant").build());
        assertTrue(result.getAtsScore().getOverall() < result.getOriginalAtsScore().getOverall());
        assertEquals("AI_HYBRID", result.getAtsScore().getAnalysisSource());
        assertEquals(30, result.getTotalTokensUsed());
    }

    static ResumeData resume(ResumeData.ResumeSection... sections) {
        var data = new ResumeData(); data.setSections(new ArrayList<>(List.of(sections))); return data;
    }
    static ResumeData.ResumeSection section(String id, String type, ResumeData.SectionItem... items) {
        var section = new ResumeData.ResumeSection(); section.setId(id); section.setType(type); section.setTitle(type);
        section.setItems(new ArrayList<>(List.of(items))); return section;
    }
    static ResumeData.SectionItem item(String id, String title, String subtitle, String date) {
        var item = new ResumeData.SectionItem(); item.setId(id); item.setTitle(title); item.setSubtitle(subtitle); item.setDate(date); return item;
    }
}
