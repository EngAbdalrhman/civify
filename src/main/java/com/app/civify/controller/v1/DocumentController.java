package com.app.civify.controller.v1;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.dto.document.MaskRequest;
import com.app.civify.dto.document.MaskResponse;
import com.app.civify.dto.document.ProcessedDocument;
import com.app.civify.entity.User;
import com.app.civify.service.user.UserService;
import com.app.civify.service.document.CvParsingService;
import com.app.civify.service.document.DocumentProcessorService;
import com.app.civify.service.document.masking.MaskingService;
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

    private final DocumentProcessorService documentProcessorService;
    private final CvParsingService cvParsingService;
    private final MaskingService maskingService;
    private final UserService userService;

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
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "request", required = false) CvParseRequest request) {

        log.info("Parsing CV: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

        User currentUser = userService.getCurrentUser();
        CvParseRequest parseRequest = request != null ? request : CvParseRequest.builder().build();

        CvParseResponse response = cvParsingService.parseCv(file, parseRequest, currentUser.getId());

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
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String languageHint) {

        log.info("Extracting text from: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

        ProcessedDocument result = documentProcessorService.process(file, languageHint);

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
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "request", required = false) MaskRequest request) {

        log.info("Masking document: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

        try {
            MaskRequest maskRequest = request != null ? request : MaskRequest.builder().build();
            MaskResponse response = maskingService.maskDocument(
                    file.getBytes(),
                    file.getContentType(),
                    maskRequest
            );

            if (response.isSuccess()) {
                return ResponseEntity.ok(CommonResponse.success("Document masked successfully", response));
            } else {
                return ResponseEntity.badRequest()
                        .body(CommonResponse.error(response.getErrorMessage(), response));
            }
        } catch (Exception e) {
            log.error("Error masking document", e);
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("Failed to mask document: " + e.getMessage()));
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
    public ResponseEntity<CommonResponse<MaskResponse>> maskText(@RequestBody MaskRequest request) {
        log.info("Masking text content, length: {} chars",
                request.getText() != null ? request.getText().length() : 0);

        if (request.getText() == null || request.getText().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("Text content is required"));
        }

        MaskResponse response = maskingService.maskText(request.getText(), request);

        return ResponseEntity.ok(CommonResponse.success("Text masked successfully", response));
    }

    @Operation(
            summary = "Get supported file types",
            description = "Returns a list of supported MIME types for document processing."
    )
    @GetMapping("/supported-types")
    public ResponseEntity<CommonResponse<Set<String>>> getSupportedTypes() {
        return ResponseEntity.ok(CommonResponse.success(documentProcessorService.getSupportedMimeTypes()));
    }
}
