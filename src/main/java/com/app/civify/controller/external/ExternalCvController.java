package com.app.civify.controller.external;

import com.app.civify.annotation.RequiresScope;
import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.cv.CvTailorResponse;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.external.ExternalCvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.app.civify.dto.external.ExternalUserProfileResponse;
import com.app.civify.dto.cv.CvScoreRequest;
import com.app.civify.dto.job.ScrapeJdRequest;
import com.app.civify.dto.job.ScrapeJdResponse;
import com.app.civify.service.external.JdScraperService;
import com.app.civify.usecase.cv.CvUseCase;
import com.app.civify.util.CommonResponse;
import jakarta.validation.Valid;

@ApiV1Controller
@RequestMapping("/external/cvs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "External CV API", description = "Endpoints for external developers using API Keys")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ExternalCvController {

    private final ExternalCvService externalCvService;
    private final JdScraperService jdScraperService;
    private final CvUseCase cvUseCase;

    @Operation(
            summary = "Mask CV (External)",
            description = "Upload a CV to mask PII and receive a masked PDF. Requires X-API-KEY header.",
            security = @SecurityRequirement(name = "api-key")
    )
    @RequiresScope("pii:mask")
    @PostMapping(value = "/mask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> maskCv(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest httpRequest) {
        
        try {
            byte[] pdfBytes = externalCvService.maskCv(file, language, userPrincipal.getUser(), httpRequest);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"masked_cv.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
            summary = "Parse CV (External)",
            description = "Upload a CV to extract structured data. Requires X-API-KEY header. Optional 'userTemplate' as a file part (JSON/YAML) to define output structure.",
            security = @SecurityRequirement(name = "api-key")
    )
    @RequiresScope("cv:parse")
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CvParseResponse> parseCv(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language,
            @RequestPart(value = "userTemplate", required = false) String userTemplate,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest httpRequest) {
        
        try {
            CvParseResponse response = externalCvService.parseCv(file, language, userTemplate, userPrincipal.getUser(), httpRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
             return ResponseEntity.badRequest().body(CvParseResponse.builder()
                     .success(false)
                     .message(e.getMessage())
                     .build());
        } catch (Exception e) {
             return ResponseEntity.internalServerError().body(CvParseResponse.builder()
                     .success(false)
                     .message("Internal Server Error: " + e.getMessage())
                     .build());
        }
    }

    @Operation(
            summary = "Tailor CV (External)",
            description = "Upload a CV and job description to get a tailored CV with ATS analysis. Requires X-API-KEY header.",
            security = @SecurityRequirement(name = "api-key")
    )
    @RequiresScope("cv:tailor")
    @PostMapping(value = "/tailor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CvTailorResponse> tailorCv(
            @RequestPart("file") MultipartFile file,
            @RequestPart("jobDescription") String jobDescription,
            @RequestParam(value = "jobTitle", required = false) String jobTitle,
            @RequestParam(value = "companyName", required = false) String companyName,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "generateCoverLetter", defaultValue = "false") boolean generateCoverLetter,
            @RequestParam(value = "includeInterviewQuestions", defaultValue = "false") boolean includeInterviewQuestions,
            @RequestParam(value = "includeRoadmap", defaultValue = "false") boolean includeRoadmap,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest httpRequest) {

        try {
            CvTailorResponse response = externalCvService.tailorCv(
                    file, jobDescription, jobTitle, companyName, language,
                    generateCoverLetter, includeInterviewQuestions, includeRoadmap,
                    userPrincipal.getUser(), httpRequest
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(CvTailorResponse.builder()
                    .build());
        } catch (Exception e) {
            log.error("Error tailoring CV: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(CvTailorResponse.builder()
                    .build());
        }
    }


    @Operation(
            summary = "Get User Profile (External)",
            description = "Get basic user profile and plan details. Requires X-API-KEY header.",
            security = @SecurityRequirement(name = "api-key")
    )
    @RequiresScope("account:read")
    @GetMapping("/user/profile")
    public ResponseEntity<ExternalUserProfileResponse> getUserProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(externalCvService.getUserProfile(userPrincipal.getUser()));
    }

    @Operation(
            summary = "Calculate ATS Score (External)",
            description = "Analyzes a CV for general ATS compatibility. Requires X-API-KEY header.",
            security = @SecurityRequirement(name = "api-key")
    )
    @RequiresScope("ats:score")
    @PostMapping("/score")
    public ResponseEntity<CommonResponse<CvTailorResponse.AtsScore>> calculateScore(
            @Valid @RequestBody CvScoreRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        CvTailorResponse.AtsScore score = cvUseCase.calculateScore(request.getResumeData(), userPrincipal.getId());
        return ResponseEntity.ok(CommonResponse.success("ATS score calculated successfully", score));
    }

    @Operation(
            summary = "Scrape Job Description (External)",
            description = "Scrape and extract structured text from a job URL."
    )
    @PostMapping("/scrape-jd")
    public ResponseEntity<ScrapeJdResponse> scrapeJobDescription(@RequestBody ScrapeJdRequest request) {
        String content = jdScraperService.scrape(request.getUrl());
        return ResponseEntity.ok(ScrapeJdResponse.builder().content(content).build());
    }
}

