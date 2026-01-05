package com.app.civify.controller.v1.resume;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.job.JobApplicationRequest;
import com.app.civify.dto.job.JobApplicationResponse;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.job.JobApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@ApiV1Controller
@RequestMapping("/job-applications")
@RequiredArgsConstructor
@Tag(name = "Job Applications", description = "Endpoints for managing job applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @Operation(summary = "Get all job applications", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    public ResponseEntity<List<JobApplicationResponse>> getAllApplications(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(jobApplicationService.getAllApplications(userPrincipal));
    }

    @Operation(summary = "Get job application by ID", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> getApplication(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(jobApplicationService.getApplication(id, userPrincipal));
    }

    @Operation(summary = "Create job application", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    public ResponseEntity<JobApplicationResponse> createApplication(@RequestBody JobApplicationRequest request,
                                                                    @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobApplicationService.createApplication(request, userPrincipal));
    }

    @Operation(summary = "Update job application", security = @SecurityRequirement(name = "bearer-jwt"))
    @PatchMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> updateApplication(
            @PathVariable Long id,
            @RequestBody JobApplicationRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(jobApplicationService.updateApplication(id, request, userPrincipal));
    }

    @Operation(summary = "Delete job application", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        jobApplicationService.deleteApplication(id, userPrincipal);
        return ResponseEntity.noContent().build();
    }
}
