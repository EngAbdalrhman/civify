package com.app.civify.controller.v1.resume;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.jobmatch.JobMatchHistoryDto;
import com.app.civify.dto.jobmatch.JobMatchRequest;
import com.app.civify.dto.jobmatch.JobMatchResult;
import com.app.civify.security.UserPrincipal;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.jobmatch.JobMatchHistoryService;
import com.app.civify.service.jobmatch.JobMatchService;
import com.app.civify.util.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiV1Controller
@RequestMapping("/job-match")
@Tag(name = "Job Match Analysis", description = "Analyzes a CV against a specific Job Description")
@RequiredArgsConstructor
public class JobMatchController {

    private final JobMatchService jobMatchService;
    private final JobMatchHistoryService jobMatchHistoryService;

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

    @Operation(
            summary = "Get Match History",
            description = "Retrieves the user's past job match results.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "CV.JOB_MATCH")
    @GetMapping("/history")
    public ResponseEntity<CommonResponse<Page<JobMatchHistoryDto>>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        
        Page<JobMatchHistoryDto> history = jobMatchHistoryService.getUserHistory(principal.getId(), pageable);
        return ResponseEntity.ok(CommonResponse.success("Job match history fetched successfully", history));
    }
}
