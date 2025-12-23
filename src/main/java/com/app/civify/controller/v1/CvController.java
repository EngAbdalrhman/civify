package com.app.civify.controller.v1;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.cv.CvTailorRequest;
import com.app.civify.dto.cv.CvTailorResponse;
import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.entity.User;
import com.app.civify.repository.UserRepository;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.cv.CvEnhancementService;
import com.app.civify.service.cvlog.CvOperationLogService;
import com.app.civify.service.document.CvParsingService;
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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class CvController {

    private final CvParsingService cvParsingService;
    private final CvEnhancementService cvEnhancementService;
    private final CvOperationLogService cvOperationLogService;
    private final UserRepository userRepository;

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
        
        long startTime = System.currentTimeMillis();
        log.info("Uploading CV: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());
        
        var request = CvParseRequest.builder()
                .language(language)
                .saveToDatabase(save)
                .build();
                
        Long userId = getCurrentUserId();
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        CvParseResponse response = cvParsingService.parseCv(file, request, userId);
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        // Log the operation
        cvOperationLogService.logUpload(
                user,
                response.getResumeData(),
                "/api/v1/cvs/upload",
                processingTime,
                response.isSuccess(),
                response.isSuccess() ? null : response.getMessage(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                file.getOriginalFilename(),
                language
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
        
        long startTime = System.currentTimeMillis();
        log.info("Tailoring CV for job: {}", 
            request.getJobTitle() != null ? request.getJobTitle() : "unspecified");
        
        Long userId = getCurrentUserId();
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        
        CvTailorResponse response = cvEnhancementService.tailorCv(request);
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        // Log the operation
        cvOperationLogService.logTailor(
                user,
                request.getResumeData(),
                "/api/v1/cvs/tailor",
                processingTime,
                true,
                null,
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                request.getJobTitle()
        );
        
        return ResponseEntity.ok(CommonResponse.success("CV tailored successfully", response));
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

