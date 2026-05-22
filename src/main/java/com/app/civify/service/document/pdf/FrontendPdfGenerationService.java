package com.app.civify.service.document.pdf;

import com.app.civify.dto.document.WatermarkConfigDto;
import com.app.civify.dto.resume.ResumeData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates PDFs by calling the Frontend's high-fidelity Puppeteer endpoint.
 * High-Fidelity = Pixel perfect, CSS Grid/Flexbox support, Modern fonts.
 */
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class FrontendPdfGenerationService implements PdfGenerationService {

    private final ThymeleafPdfGenerationService fallbackService;
    
    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${pdf.internal.secret:}")
    private String pdfInternalSecret;

    @Override
    public byte[] generatePdf(ResumeData resumeData, String templateName, WatermarkConfigDto watermark) {
        log.info("Requesting High-Fidelity PDF from Frontend: {} using template: {}", frontendUrl, templateName);

        try {
            RestClient restClient = RestClient.create();

            Map<String, Object> body = new HashMap<>();
            body.put("resumeData", resumeData);
            body.put("template", templateName != null ? templateName : "modern");
            body.put("filename", "resume");
            if (watermark != null) {
                body.put("watermark", watermark);
            }

            var request = restClient.post()
                    .uri(frontendUrl + "/api/generate-pdf")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);

            if (pdfInternalSecret != null && !pdfInternalSecret.isBlank()) {
                request = request.header("X-Pdf-Internal-Token", pdfInternalSecret);
            }

            return request.retrieve().body(byte[].class);

        } catch (Exception e) {
            log.error("Frontend PDF generation failed: {}. Falling back to legacy renderer.", e.getMessage());
            return fallbackService.generatePdf(resumeData, templateName, watermark);
        }
    }
}
