package com.app.civify.service.document.masking;

import com.app.civify.dto.document.MaskRequest;
import com.app.civify.dto.document.MaskRequest.MaskType;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.service.ai.AiService;
import com.app.civify.service.document.DocumentProcessorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResumeMaskingTest {
    final AiService ai = mock(AiService.class);
    final ObjectMapper mapper = new ObjectMapper();
    final MaskingServiceImpl service = new MaskingServiceImpl(mock(DocumentProcessorService.class), ai, mapper);

    @Test void masksRepeatedContactsAndLabeledPersonalDetailsWithoutChangingSource() throws Exception {
        var source = fixture();
        var masked = service.maskResumeData(source, MaskRequest.builder().build());
        String json = mapper.writeValueAsString(masked);
        for (String pii : List.of("Sample Person", "sample@example.com", "01012345678", "9/9/1999")) assertFalse(json.contains(pii), pii);
        assertEquals("[ADDRESS_REDACTED]", masked.getPersonalInfo().getCity());
        assertTrue(json.contains("2021-2023"), "employment dates must survive");
        assertEquals("Sample Person", source.getPersonalInfo().getFullName());
        assertEquals("9/9/1999", source.getSections().getFirst().getItems().getFirst().getSubtitle());
        verifyNoInteractions(ai);
    }

    @Test void selectedMaskTypesKeepOtherDetails() {
        var masked = service.maskResumeData(fixture(), MaskRequest.builder().maskTypes(List.of(MaskType.EMAIL)).build());
        assertEquals("Sample Person", masked.getPersonalInfo().getFullName());
        assertEquals("[EMAIL_REDACTED]", masked.getPersonalInfo().getEmail());
        assertEquals("Cairo", masked.getPersonalInfo().getCity());
        assertEquals("9/9/1999", masked.getSections().getFirst().getItems().getFirst().getSubtitle());
    }

    @Test void demographicRedactionDoesNotRemoveUnrelatedTechnicalPhrases() {
        var source = fixture();
        var demographic = source.getSections().getFirst().getItems().getFirst();
        demographic.setTitle("Marital Status"); demographic.setSubtitle("Single");
        source.getPersonalInfo().setSummary("Implemented Single Sign On for enterprise applications.");
        var masked = service.maskResumeData(source, MaskRequest.builder().build());
        assertEquals("[PERSONAL_DETAIL_REDACTED]", masked.getSections().getFirst().getItems().getFirst().getSubtitle());
        assertTrue(masked.getPersonalInfo().getSummary().contains("Single Sign On"));
    }

    private ResumeData fixture() {
        var data = new ResumeData(); var info = data.getPersonalInfo();
        info.setFullName("Sample Person"); info.setEmail("sample@example.com"); info.setPhone("01012345678"); info.setCity("Cairo");
        info.setSummary("Sample Person can be reached at sample@example.com or 01012345678.");
        var section = new ResumeData.ResumeSection(); section.setType("custom");
        var birth = new ResumeData.SectionItem(); birth.setTitle("Date of Birth"); birth.setSubtitle("9/9/1999");
        var work = new ResumeData.SectionItem(); work.setTitle("Consultant"); work.setDate("2021-2023"); work.setDescription("Contact sample@example.com");
        section.setItems(List.of(birth, work)); data.setSections(List.of(section)); return data;
    }
}
