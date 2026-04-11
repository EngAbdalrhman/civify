package com.app.civify.controller.v1.resume;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.jobmatch.JobMatchRequest;
import com.app.civify.dto.jobmatch.JobMatchResult;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.exception.BadRequestException;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.external.JdScraperService;
import com.app.civify.service.jobmatch.JobMatchService;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.util.CommonResponse;
import com.app.civify.entity.enums.TokenUsageType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiV1Controller
@RequestMapping("/job-match")
@Tag(name = "Job Match Analysis", description = "Analyzes a CV against a specific Job Description")
@RequiredArgsConstructor
public class JobMatchController {

    private final JobMatchService jobMatchService;
    private final JdScraperService jdScraperService;
    private final SubscriptionService subscriptionService;

    @Operation(
            summary = "Analyze Match",
            description = "Analyzes how well a CV matches a raw job description or a JD from a URL.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("/analyze")
    public ResponseEntity<CommonResponse<JobMatchResult>> analyzeMatch(
            @Valid @RequestBody JobMatchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long userId = principal.getId();

        // Get the requested Resume
        ResumeData resumeData = request.getResumeData();
        if (resumeData == null) {
            throw new BadRequestException("Resume data is required");
        }

        // Determine job description content (scrape URL or raw text)
        String rawJd;
        if (request.getJobUrl() != null && !request.getJobUrl().trim().isEmpty()) {
            rawJd = jdScraperService.scrape(request.getJobUrl());
        } else if (request.getJobDescription() != null && !request.getJobDescription().trim().isEmpty()) {
            rawJd = request.getJobDescription();
        } else {
            throw new BadRequestException("Either jobDescription or jobUrl must be provided.");
        }

        // Perform analysis (incrementUsage already validates the limit internally)
        subscriptionService.incrementUsage(userId, "JOB_MATCH");

        JobMatchResult result = jobMatchService.analyzeMatch(resumeData, rawJd);

        // Charge AI tokens if any were used
        if (result.getTotalTokensUsed() > 0) {
            try {
                subscriptionService.chargeTokens(userId, result.getTotalTokensUsed(), TokenUsageType.CV_TAILOR, "Job Match Analysis");
            } catch (Exception e) {
                // Token charging failure is non-critical
            }
        }

        return ResponseEntity.ok(CommonResponse.success("Match analysis complete", result));
    }
}
