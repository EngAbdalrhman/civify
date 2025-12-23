package com.app.civify.controller.external;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.dto.document.MaskRequest;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.entity.User;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.cvlog.CvOperationLogService;
import com.app.civify.service.document.CvParsingService;
import com.app.civify.service.document.masking.MaskingService;
import com.app.civify.service.document.pdf.PdfGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@ApiV1Controller
@RestController
@RequestMapping("/external/cvs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "External CV API", description = "Endpoints for external developers using API Keys")
public class ExternalCvController {

    private final CvParsingService cvParsingService;
    private final MaskingService maskingService;
    private final PdfGenerationService pdfGenerationService;
    private final CvOperationLogService cvOperationLogService;

    @Operation(
            summary = "Mask CV (External)",
            description = "Upload a CV to mask PII and receive a masked PDF. Requires X-API-KEY header.",
            security = @SecurityRequirement(name = "api-key")
    )
    @PostMapping(value = "/mask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> maskCv(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language,
            HttpServletRequest httpRequest) {
        
        long startTime = System.currentTimeMillis();
        User user = getCurrentUser();
        ResumeData originalResumeData = null;
        
        try {
            // 1. Parse
            long parseStart = System.currentTimeMillis();
            var parseRequest = CvParseRequest.builder()
                    .language(language)
                    .saveToDatabase(false)
                    .piiOnly(true) // Optimization: Only extract PII for masking
                    .build();
            
            var parseResponse = cvParsingService.parseCv(file, parseRequest, null);
            long parseTime = System.currentTimeMillis() - parseStart;
            
            if (!parseResponse.isSuccess() || parseResponse.getResumeData() == null) {
                long processingTime = System.currentTimeMillis() - startTime;
                cvOperationLogService.logMask(
                        user, null, "/api/v1/external/cvs/mask", processingTime,
                        false, "Failed to parse CV",
                        getClientIp(httpRequest), httpRequest.getHeader("User-Agent"),
                        file.getOriginalFilename()
                );
                return ResponseEntity.badRequest().body(null);
            }
            
            originalResumeData = parseResponse.getResumeData();

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

            // Log successful operation
            cvOperationLogService.logMask(
                    user, originalResumeData, "/api/v1/external/cvs/mask", totalTime,
                    true, null,
                    getClientIp(httpRequest), httpRequest.getHeader("User-Agent"),
                    file.getOriginalFilename()
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"masked_cv.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            cvOperationLogService.logMask(
                    user, originalResumeData, "/api/v1/external/cvs/mask", processingTime,
                    false, e.getMessage(),
                    getClientIp(httpRequest), httpRequest.getHeader("User-Agent"),
                    file.getOriginalFilename()
            );
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
            @RequestParam(value = "language", required = false) String language,
            HttpServletRequest httpRequest) {
        
        long startTime = System.currentTimeMillis();
        User user = getCurrentUser();
        
        var request = CvParseRequest.builder()
                .language(language)
                .saveToDatabase(false)
                .build();

        CvParseResponse response = cvParsingService.parseCv(file, request, null);
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        // Log the operation
        cvOperationLogService.logParse(
                user, response.getResumeData(), "/api/v1/external/cvs/parse", processingTime,
                response.isSuccess(), response.isSuccess() ? null : response.getMessage(),
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"),
                file.getOriginalFilename(), language
        );

        return ResponseEntity.ok(response);
    }

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getUser();
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

