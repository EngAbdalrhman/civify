package com.app.civify.controller.v1.resume;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.dto.document.MaskRequest;
import com.app.civify.dto.document.MaskResponse;
import com.app.civify.dto.document.ProcessedDocument;
import com.app.civify.entity.User;
import com.app.civify.security.UserPrincipal;
import com.app.civify.usecase.document.DocumentUseCase;
import com.app.civify.util.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Controller for document processing operations including CV parsing and
 * masking.
 */
@ApiV1Controller
@RequestMapping("/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document Processing", description = "CV parsing, OCR, and masking endpoints")
public class DocumentController {

    private final DocumentUseCase documentUseCase;

    @Operation(
            summary = "Parse CV/Resume",
            description = "Parses a CV/Resume file (PDF, DOCX, or image) and extracts structured data. "
            + "Supports OCR for scanned documents and Arabic language.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CV parsed successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = CvParseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or unsupported format"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<CvParseResponse>> parseCv(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "request", required = false) CvParseRequest request) {

        log.info("Request to parse CV: {}", file.getOriginalFilename());
        
        Long userId = (userPrincipal != null && userPrincipal.getUser() != null) 
                ? userPrincipal.getUser().getId() 
                : null;
        
        CvParseResponse response = documentUseCase.parseCv(file, request, userId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(CommonResponse.success("CV parsed successfully", response));
        } else {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error(response.getMessage(), response));
        }
    }

    @Operation(
            summary = "Extract text from document",
            description = "Extracts raw text from a document (PDF, DOCX, or image) using OCR if needed.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Text extracted successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ProcessedDocument.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or unsupported format"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<ProcessedDocument>> extractText(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String languageHint) {

        log.info("Request to extract text from: {}", file.getOriginalFilename());

        Long userId = (userPrincipal != null && userPrincipal.getUser() != null) 
                ? userPrincipal.getUser().getId() 
                : null;

        ProcessedDocument result = documentUseCase.extractText(file, languageHint, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(CommonResponse.success("Text extracted successfully", result));
        } else {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error(result.getErrorMessage(), result));
        }
    }

    @Operation(
            summary = "Mask PII in document",
            description = "Masks personally identifiable information (PII) in a document. "
            + "Supports masking names, emails, phones, addresses, and more in both English and Arabic.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Document masked successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = MaskResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or unsupported format"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(value = "/mask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<MaskResponse>> maskDocument(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "request", required = false) MaskRequest request) {

        log.info("Request to mask document: {}", file.getOriginalFilename());

        Long userId = (userPrincipal != null && userPrincipal.getUser() != null) 
                ? userPrincipal.getUser().getId() 
                : null;
                
        MaskResponse response = documentUseCase.maskDocument(file, request, userId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(CommonResponse.success("Document masked successfully", response));
        } else {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error(response.getErrorMessage(), response));
        }
    }

    @Operation(
            summary = "Mask text content",
            description = "Masks PII in plain text content. Useful for masking already extracted text.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Text masked successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = MaskResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/mask-text")
    public ResponseEntity<CommonResponse<MaskResponse>> maskText(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody MaskRequest request) {
        
        Long userId = (userPrincipal != null && userPrincipal.getUser() != null) 
                ? userPrincipal.getUser().getId() 
                : null;
                
        MaskResponse response = documentUseCase.maskText(request, userId);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(CommonResponse.success("Text masked successfully", response));
        } else {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error(response.getErrorMessage()));
        }
    }

    @Operation(
            summary = "Get supported file types",
            description = "Returns a list of supported MIME types for document processing."
    )
    @GetMapping("/supported-types")
    public ResponseEntity<CommonResponse<Set<String>>> getSupportedTypes() {
        return ResponseEntity.ok(CommonResponse.success(documentUseCase.getSupportedMimeTypes()));
    }
}
