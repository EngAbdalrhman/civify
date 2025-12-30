package com.app.civify.controller.v1.resume;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.job.JobApplicationRequest;
import com.app.civify.dto.job.JobApplicationResponse;
import com.app.civify.service.job.JobApplicationService;
import com.app.civify.util.security.EntityType;
import com.app.civify.util.security.IdEncodingUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@ApiV1Controller
@RequestMapping("/job-applications")
@RequiredArgsConstructor
@Tag(name = "Job Applications", description = "Endpoints for managing job applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;
    private final IdEncodingUtil idEncodingUtil;

    @Operation(summary = "Get all job applications", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    public ResponseEntity<List<JobApplicationResponse>> getAllApplications() {
        return ResponseEntity.ok(jobApplicationService.getAllApplications());
    }

    @Operation(summary = "Get job application by ID", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> getApplication(
            @Parameter(description = "Encoded job application ID") @PathVariable String id) {
        Long decodedId = idEncodingUtil.decode(id, EntityType.JOB_APPLICATION);
        return ResponseEntity.ok(jobApplicationService.getApplication(decodedId));
    }

    @Operation(summary = "Create job application", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    public ResponseEntity<JobApplicationResponse> createApplication(@RequestBody JobApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobApplicationService.createApplication(request));
    }

    @Operation(summary = "Update job application", security = @SecurityRequirement(name = "bearer-jwt"))
    @PatchMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> updateApplication(
            @Parameter(description = "Encoded job application ID") @PathVariable String id,
            @RequestBody JobApplicationRequest request) {
        Long decodedId = idEncodingUtil.decode(id, EntityType.JOB_APPLICATION);
        return ResponseEntity.ok(jobApplicationService.updateApplication(decodedId, request));
    }

    @Operation(summary = "Delete job application", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @Parameter(description = "Encoded job application ID") @PathVariable String id) {
        Long decodedId = idEncodingUtil.decode(id, EntityType.JOB_APPLICATION);
        jobApplicationService.deleteApplication(decodedId);
        return ResponseEntity.noContent().build();
    }
}

