package com.app.civify.controller.external;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.entity.User;
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

    private final ExternalCvService externalCvService;

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
        
        try {
            byte[] pdfBytes = externalCvService.maskCv(file, language, getCurrentUser(), httpRequest);
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
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CvParseResponse> parseCv(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language,
            @RequestPart(value = "userTemplate", required = false) String userTemplate,
            HttpServletRequest httpRequest) {
        
        try {
            CvParseResponse response = externalCvService.parseCv(file, language, userTemplate, getCurrentUser(), httpRequest);
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

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getUser();
        }
        return null;
    }
}

