package com.app.civify.service.document.pdf;

import com.app.civify.dto.document.WatermarkConfigDto;
import com.app.civify.dto.resume.ResumeData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;

/**
 * Implementation of PdfGenerationService using Thymeleaf for templating
 * and Flying Saucer (OpenHTMLtoPDF) for PDF generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThymeleafPdfGenerationService implements PdfGenerationService {

    private final TemplateEngine templateEngine;

    @Override
    public byte[] generatePdf(ResumeData resumeData, String templateName, WatermarkConfigDto watermark) {
        log.info("Generating PDF using template: {}", templateName);
        
        try {
            Context context = new Context();
            context.setVariable("resume", resumeData);
            if (watermark != null && watermark.isEnabled()) {
                context.setVariable("watermark", watermark);
            }
            
            String htmlContent = templateEngine.process("cv/" + templateName, context);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}
