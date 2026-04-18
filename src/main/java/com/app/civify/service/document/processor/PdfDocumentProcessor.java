package com.app.civify.service.document.processor;

import com.app.civify.dto.document.DocumentType;
import com.app.civify.dto.document.OcrResult;
import com.app.civify.dto.document.ProcessedDocument;
import com.app.civify.exception.BadRequestException;
import com.app.civify.service.document.DocumentProcessor;
import com.app.civify.service.document.ocr.SmartOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Processor for PDF documents. Handles both text-based and scanned
 * (image-based) PDFs.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PdfDocumentProcessor implements DocumentProcessor {

    private final SmartOcrService ocrService;

    private static final int MIN_TEXT_LENGTH_THRESHOLD = 100;
    private static final float OCR_DPI = 300f;
    private static final int MAX_PAGES = 8;


    @Override
    public ProcessedDocument process(MultipartFile file) {
        return process(file, null);
    }

    @Override
    public ProcessedDocument process(MultipartFile file, String languageHint) {
        long startTime = System.currentTimeMillis();

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            int pageCount = document.getNumberOfPages();
            log.info("Processing PDF: {}, pages: {}", file.getOriginalFilename(), pageCount);
            if (pageCount > MAX_PAGES){
                throw new BadRequestException("Can't proceed a cv larger than " + MAX_PAGES + " pages");
            }

            // First, try direct text extraction
            PDFTextStripper stripper = new PDFTextStripper();
            String extractedText = stripper.getText(document);

            boolean isScanned = isScannedPdf(document, extractedText);
            String processingMethod;
            double confidence = 1.0;
            List<String> warnings = new ArrayList<>();

            if (isScanned) {
                log.info("PDF appears to be scanned (text length: {}), using OCR", 
                        extractedText != null ? extractedText.length() : 0);
                long ocrStart = System.currentTimeMillis();
                OcrResult ocrResult = processWithOcr(document, languageHint);
                log.info("OCR Processing Time: {}ms", System.currentTimeMillis() - ocrStart);

                if (ocrResult.isSuccess() && ocrResult.hasText()) {
                    extractedText = ocrResult.getText();
                    confidence = ocrResult.getConfidence();
                    processingMethod = "OCR_" + ocrResult.getOcrProvider();
                } else {
                    warnings.add("OCR processing had issues: " + ocrResult.getErrorMessage());
                    processingMethod = "OCR_PARTIAL";
                }
            } else {
                log.info("PDF is text-based (text length: {}), skipping OCR", extractedText.length());
                processingMethod = "DIRECT_EXTRACTION";
            }

            long processingTime = System.currentTimeMillis() - startTime;

            return ProcessedDocument.builder()
                    .rawText(extractedText)
                    .documentType(isScanned ? DocumentType.PDF_SCANNED : DocumentType.PDF)
                    .processingMethod(processingMethod)
                    .pageCount(pageCount)
                    .confidence(confidence)
                    .warnings(warnings)
                    .processingTimeMs(processingTime)
                    .originalFilename(file.getOriginalFilename())
                    .fileSizeBytes(file.getSize())
                    .build();

        } catch (IOException e) {
            log.error("Error processing PDF: {}", e.getMessage(), e);
            return ProcessedDocument.builder()
                    .rawText("")
                    .documentType(DocumentType.PDF)
                    .processingMethod("ERROR")
                    .confidence(0.0)
                    .warnings(List.of("Failed to process PDF: " + e.getMessage()))
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .originalFilename(file.getOriginalFilename())
                    .build();
        }
    }

    @Override
    public boolean supports(DocumentType documentType) {
        return documentType.isPdf();
    }

    /**
     * Determine if a PDF is scanned (image-based) rather than text-based.
     */
    private boolean isScannedPdf(PDDocument document, String extractedText) {
        // If we got substantial text, it's likely not scanned
        if (extractedText != null && extractedText.trim().length() >= MIN_TEXT_LENGTH_THRESHOLD) {
            return false;
        }

        // Check if pages contain images
        try {
            for (PDPage page : document.getPages()) {
                PDResources resources = page.getResources();
                if (resources != null) {
                    for (var name : resources.getXObjectNames()) {
                        if (resources.getXObject(name) instanceof PDImageXObject) {
                            return true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Error checking for images in PDF: {}", e.getMessage());
        }

        return extractedText == null || extractedText.trim().length() < MIN_TEXT_LENGTH_THRESHOLD;
    }

    /**
     * Process PDF pages using OCR.
     */
    private OcrResult processWithOcr(PDDocument document, String languageHint) {
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder fullText = new StringBuilder();
            double totalConfidence = 0.0;
            int processedPages = 0;
            String ocrProvider = null;

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, OCR_DPI);
                byte[] imageBytes = bufferedImageToBytes(image);

                OcrResult pageResult = ocrService.extractText(imageBytes, "image/png", languageHint);

                if (pageResult.isSuccess()) {
                    fullText.append(pageResult.getText()).append("\n\n");
                    totalConfidence += pageResult.getConfidence();
                    processedPages++;
                    ocrProvider = pageResult.getOcrProvider();
                }
            }

            double avgConfidence = processedPages > 0 ? totalConfidence / processedPages : 0.0;

            return OcrResult.builder()
                    .text(fullText.toString().trim())
                    .confidence(avgConfidence)
                    .success(true)
                    .ocrProvider(ocrProvider)
                    .build();

        } catch (IOException e) {
            log.error("OCR processing failed: {}", e.getMessage(), e);
            return OcrResult.error("OCR processing failed: " + e.getMessage());
        }
    }

    private byte[] bufferedImageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}
