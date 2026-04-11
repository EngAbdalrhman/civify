package com.app.civify.controller.v1.resume;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.jobmatch.JobMatchRequest;
import com.app.civify.dto.jobmatch.JobMatchResult;
import com.app.civify.security.UserPrincipal;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.jobmatch.JobMatchService;
import com.app.civify.util.CommonResponse;
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

    @Operation(
            summary = "Analyze Match",
            description = "Analyzes how well a CV matches a raw job description or a JD from a URL.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "CV.JOB_MATCH")
    @PostMapping("/analyze")
    public ResponseEntity<CommonResponse<JobMatchResult>> analyzeMatch(
            @Valid @RequestBody JobMatchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        JobMatchResult result = jobMatchService.analyzeMatch(principal.getId(), request);
        return ResponseEntity.ok(CommonResponse.success("Match analysis complete", result));
    }
}
