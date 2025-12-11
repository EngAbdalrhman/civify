package com.app.civify.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.app.civify.service.document.CvParsingService;
import com.app.civify.service.document.masking.MaskingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@ApiV1Controller
@RequestMapping("/cvs")
@Tag(name = "CV Management", description = "CV upload and tailoring endpoints")
@RequiredArgsConstructor
public class CvController {

    private final CvParsingService cvParsingService;
    private final MaskingService maskingService;
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
    public ResponseEntity<com.app.civify.dto.document.CvParseResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "save", defaultValue = "true") boolean save) {
        
        var request = com.app.civify.dto.document.CvParseRequest.builder()
                .language(language)
                .saveToDatabase(save)
                .build();
                
        // Get authenticated user ID
        Long userId = null;
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.app.civify.security.UserPrincipal) {
            userId = ((com.app.civify.security.UserPrincipal) authentication.getPrincipal()).getId();
        }
        
        return ResponseEntity.ok(cvParsingService.parseCv(file, request, userId));
    }

    @Operation(
            summary = "Mask CV content",
            description = "Masks sensitive information in the CV.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping(value = "/mask", consumes = "multipart/form-data")
    public ResponseEntity<com.app.civify.dto.document.MaskResponse> mask(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language) {
            
        var request = com.app.civify.dto.document.MaskRequest.builder()
                .build(); // Use defaults
                
        // Read file bytes
        try {
            byte[] fileData = file.getBytes();
            return ResponseEntity.ok(maskingService.maskDocument(fileData, file.getContentType(), request));
        } catch (java.io.IOException e) {
            return ResponseEntity.badRequest().build();
        }
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
