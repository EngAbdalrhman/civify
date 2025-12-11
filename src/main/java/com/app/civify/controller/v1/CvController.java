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
    private final com.app.civify.service.document.pdf.PdfGenerationService pdfGenerationService;

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
            summary = "Mask CV content and generate PDF",
            description = "Masks sensitive information in the CV and returns a masked PDF.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping(value = "/mask", consumes = "multipart/form-data")
    public ResponseEntity<byte[]> mask(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language) {
            
        try {
            // 1. Parse the CV to get structured data
            var parseRequest = com.app.civify.dto.document.CvParseRequest.builder()
                    .language(language)
                    .saveToDatabase(false) // Don't save intermediate parsing
                    .build();
            
            // Get user ID if available (optional for masking tool)
            Long userId = null;
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof com.app.civify.security.UserPrincipal) {
                userId = ((com.app.civify.security.UserPrincipal) authentication.getPrincipal()).getId();
            }

            var parseResponse = cvParsingService.parseCv(file, parseRequest, userId);
            if (!parseResponse.isSuccess() || parseResponse.getResumeData() == null) {
                return ResponseEntity.badRequest().body(null);
            }

            // 2. Mask the structured data
            var maskRequest = com.app.civify.dto.document.MaskRequest.builder().build();
            var maskedData = maskingService.maskResumeData(parseResponse.getResumeData(), maskRequest);

            // 3. Generate PDF
            byte[] pdfBytes = pdfGenerationService.generatePdf(maskedData, "modern");

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"masked_cv.pdf\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
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
