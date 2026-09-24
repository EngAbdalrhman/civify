package com.app.civify.service.external;

import com.app.civify.dto.cv.CvTailorRequest;
import com.app.civify.dto.cv.CvTailorResponse;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.TokenUsageType;
import com.app.civify.service.cv.CvEnhancementService;
import com.app.civify.service.cvlog.CvOperationLogService;
import com.app.civify.service.document.CvParsingService;
import com.app.civify.service.document.WatermarkPolicyService;
import com.app.civify.service.document.masking.MaskingService;
import com.app.civify.service.document.pdf.PdfGenerationService;
import com.app.civify.service.subscription.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalCvServiceTest {
    @Mock CvParsingService parsing;
    @Mock CvEnhancementService enhancement;
    @Mock MaskingService masking;
    @Mock PdfGenerationService pdf;
    @Mock WatermarkPolicyService watermark;
    @Mock CvOperationLogService operations;
    @Mock SubscriptionService subscriptions;
    @Mock ObjectMapper mapper;
    @InjectMocks ExternalCvService service;

    @Test
    void structuredInputSkipsParsingAndChargesTailoringOnce() {
        var user = new User();
        var resume = new ResumeData();
        var request = CvTailorRequest.builder().resumeData(resume).jobDescription("Developer").build();
        when(enhancement.tailorCv(any())).thenReturn(CvTailorResponse.builder().tailoredCv(resume).totalTokensUsed(42).build());
        var result = service.tailorCv(request, user, new MockHttpServletRequest());
        assertSame(resume, result.getTailoredCv());
        verifyNoInteractions(parsing);
        verify(enhancement).tailorCv(argThat(input -> input.getResumeData() == resume && input.getJobDescription().equals("Developer")));
        verify(subscriptions).chargeTokens(eq(user.getId()), eq(42), eq(TokenUsageType.EXTERNAL_API), anyString());
    }

    @Test
    void originalFileStillParsesOnceAndUsesTheSameTailoringPath() {
        var user = new User();
        var resume = new ResumeData();
        var file = new MockMultipartFile("file", "resume.txt", "text/plain", "Fixture".getBytes());
        when(parsing.parseCv(eq(file), any(), eq(user.getId())))
                .thenReturn(CvParseResponse.builder().success(true).resumeData(resume).build());
        when(enhancement.tailorCv(any())).thenReturn(CvTailorResponse.builder().tailoredCv(resume).totalTokensUsed(42).build());
        service.tailorCv(file, "Developer", null, null, "en", false, false, false, user, new MockHttpServletRequest());
        verify(parsing).parseCv(eq(file), any(), eq(user.getId()));
        verify(enhancement).tailorCv(argThat(input -> input.getResumeData() == resume));
        verify(subscriptions).chargeTokens(eq(user.getId()), eq(42), eq(TokenUsageType.EXTERNAL_API), anyString());
    }

    @Test
    void pdfPolicyUsesTheAuthenticatedUserAndExistingResumeEntitlement() {
        var user = new User();
        service.getWatermark(user, "existing-resume");
        verify(watermark).resolveForUserAndResume(user.getId(), "existing-resume");
        verifyNoInteractions(parsing, enhancement, subscriptions);
    }

    @Test
    void invalidInputDoesNotConsumeCredits() {
        assertThrows(IllegalArgumentException.class, () -> service.tailorCv(
                CvTailorRequest.builder().resumeData(new ResumeData()).jobDescription(" ").build(), new User(), new MockHttpServletRequest()));
        verifyNoInteractions(parsing, enhancement, subscriptions);
    }
}
