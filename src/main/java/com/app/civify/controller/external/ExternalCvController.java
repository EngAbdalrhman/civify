package com.app.civify.controller.external;

import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.dto.document.MaskRequest;
import com.app.civify.service.document.CvParsingService;
import com.app.civify.service.document.masking.MaskingService;
import com.app.civify.service.document.pdf.PdfGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/external/cvs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "External CV API", description = "Endpoints for external developers using API Keys")
public class ExternalCvController {

    private final CvParsingService cvParsingService;
    private final MaskingService maskingService;
    private final PdfGenerationService pdfGenerationService;

    @Operation(
            summary = "Mask CV (External)",
            description = "Upload a CV to mask PII and receive a masked PDF. Requires X-API-KEY header.",
            security = @SecurityRequirement(name = "api-key")
    )
    @PostMapping(value = "/mask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> maskCv(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language) {
        
        try {
            long startTime = System.currentTimeMillis();

            // 1. Parse
            long parseStart = System.currentTimeMillis();
            var parseRequest = CvParseRequest.builder()
                    .language(language)
                    .saveToDatabase(false)
                    .piiOnly(true) // Optimization: Only extract PII for masking
                    .build();
            
            // Note: External API users don't have a 'User' context for saving CVs in the same way, 
            // so we pass null for userId or handle it if needed.
            var parseResponse = cvParsingService.parseCv(file, parseRequest, null);
            long parseTime = System.currentTimeMillis() - parseStart;
            
            if (!parseResponse.isSuccess() || parseResponse.getResumeData() == null) {
                return ResponseEntity.badRequest().body(null);
            }

            // 2. Mask
            long maskStart = System.currentTimeMillis();
            var maskRequest = MaskRequest.builder().build();
            var maskedData = maskingService.maskResumeData(parseResponse.getResumeData(), maskRequest);
            long maskTime = System.currentTimeMillis() - maskStart;

            // 3. Generate PDF
            long pdfStart = System.currentTimeMillis();
            byte[] pdfBytes = pdfGenerationService.generatePdf(maskedData, "modern");
            long pdfTime = System.currentTimeMillis() - pdfStart;

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("External Mask API Timing: Total=" + totalTime + "ms, Parse=" + parseTime + "ms, Mask=" + maskTime + "ms, PDF=" + pdfTime + "ms");

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
            description = "Upload a CV to extract structured data. Requires X-API-KEY header.",
            security = @SecurityRequirement(name = "api-key")
    )
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CvParseResponse> parseCv(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language) {
        
        var request = CvParseRequest.builder()
                .language(language)
                .saveToDatabase(false) // External API usually just wants the data
                .build();

        return ResponseEntity.ok(cvParsingService.parseCv(file, request, null));
    }
}
