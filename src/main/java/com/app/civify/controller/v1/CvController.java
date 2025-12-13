package com.app.civify.controller.v1;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.document.CvParsingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@ApiV1Controller
@RequestMapping("/cvs")
@Tag(name = "CV Management", description = "CV upload and tailoring endpoints")
@RequiredArgsConstructor
public class CvController {

    private final CvParsingService cvParsingService;
    private final com.app.civify.service.cv.CvEnhancementService cvEnhancementService;

    @Operation(
            summary = "Upload CV file",
            description = "Uploads a CV file for processing and storage.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CV uploaded successfully",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid file or request",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json"))
    })
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<CvParseResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "save", defaultValue = "true") boolean save) {
        
        var request = CvParseRequest.builder()
                .language(language)
                .saveToDatabase(save)
                .build();
                
        // Get authenticated user ID
        Long userId = null;
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            userId = ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        
        return ResponseEntity.ok(cvParsingService.parseCv(file, request, userId));
    }

    @Operation(
            summary = "Enhance CV",
            description = "Enhances CV data with AI (Roadmap, Interview Questions, Cover Letter).",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("/enhance")
    public ResponseEntity<java.util.Map<String, Object>> enhance(
            @RequestBody com.app.civify.dto.resume.ResumeData resumeData,
            @RequestParam(value = "jobDescription", required = false) String jobDescription,
            @RequestParam(value = "generateCoverLetter", defaultValue = "false") boolean generateCoverLetter) {
        
        return ResponseEntity.ok(cvEnhancementService.enhanceCv(resumeData, jobDescription, generateCoverLetter));
    }

    @Operation(
            summary = "Tailor CV for job",
            description = "Tailors an existing CV to match a specific job description using AI.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CV tailored successfully",
                content = @Content(mediaType = "text/plain")),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "CV not found",
                content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/{id}/tailor")
    public ResponseEntity<String> tailor(@PathVariable Long id, @RequestBody String jobDescription) {
        // stub - implementation depends on TailoringService
        return ResponseEntity.ok("tailored");
    }
}
