package com.app.civify.controller.v1.resume;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.cv.CvScoreRequest;
import com.app.civify.dto.cv.CvTailorRequest;
import com.app.civify.dto.cv.CvTailorResponse;
import com.app.civify.dto.cv.CvTailorResponse.AtsScore;
import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.security.UserPrincipal;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.usecase.cv.CvUseCase;
import com.app.civify.util.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for CV upload and tailoring operations.
 */
@ApiV1Controller
@RequestMapping("/cvs")
@Tag(name = "CV Management", description = "CV upload and tailoring endpoints")
@RequiredArgsConstructor
public class CvController {

    private final CvUseCase cvUseCase;

    @Operation(
            summary = "Upload and parse CV",
            description = "Uploads a CV file (PDF, DOCX, or image) and extracts structured data using AI. " +
                    "Returns parsed resume data along with PII field paths for frontend masking.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CV parsed successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = CvParseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or unsupported format"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<CommonResponse<CvParseResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "save", defaultValue = "true") boolean save,
            HttpServletRequest httpRequest) {
        
        var request = CvParseRequest.builder()
                .language(language)
                .saveToDatabase(save)
                .build();
                
        Long userId = getCurrentUserId();
        CvParseResponse response = cvUseCase.uploadCv(
                file, 
                request, 
                userId, 
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(CommonResponse.success("CV parsed successfully", response));
        } else {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error(response.getMessage(), response));
        }
    }

    @Operation(
            summary = "Tailor CV for job",
            description = "Tailors a CV to match a specific job description using AI. " +
                    "Returns tailored CV, ATS score, and optionally interview prep, roadmap, and cover letter.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CV tailored successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = CvTailorResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/tailor")
    public ResponseEntity<CommonResponse<CvTailorResponse>> tailor(
            @Valid @RequestBody CvTailorRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = getCurrentUserId();
        CvTailorResponse response = cvUseCase.tailorCv(
                request, 
                userId, 
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );
        
        return ResponseEntity.ok(CommonResponse.success("CV tailored successfully", response));
    }

    @Operation(
            summary = "Translate CV",
            description = "Translates a CV into a target language using AI.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CV translated successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = com.app.civify.dto.cv.CvTranslateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - CV.TRANSLATE permission required")
    })
    @CivifyAuthorization(permission = "CV.TRANSLATE")
    @PostMapping("/translate")
    public ResponseEntity<CommonResponse<com.app.civify.dto.cv.CvTranslateResponse>> translate(
            @Valid @RequestBody com.app.civify.dto.cv.CvTranslateRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = getCurrentUserId();
        com.app.civify.dto.cv.CvTranslateResponse response = cvUseCase.translateCv(
                request, 
                userId, 
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );
        
        return ResponseEntity.ok(CommonResponse.success("CV translated successfully", response));
    }

    @Operation(
            summary = "Calculate general ATS score",
            description = "Analyzes a CV for general ATS compatibility based on industry best practices. " +
                    "Evaluates content quality, structure, and template compatibility without needing a job description.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ATS score calculated successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AtsScore.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/score")
    public ResponseEntity<CommonResponse<AtsScore>> calculateScore(
            @Valid @RequestBody CvScoreRequest request) {
        
        Long userId = getCurrentUserId();
        AtsScore score = cvUseCase.calculateScore(request.getResumeData(), userId);
        return ResponseEntity.ok(CommonResponse.success("ATS score calculated successfully", score));
    }

    private Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}


