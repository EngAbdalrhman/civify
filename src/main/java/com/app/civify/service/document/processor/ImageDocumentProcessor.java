package com.app.civify.service.document.processor;

import com.app.civify.dto.document.DocumentType;
import com.app.civify.dto.document.OcrResult;
import com.app.civify.dto.document.ProcessedDocument;
import com.app.civify.service.document.DocumentProcessor;
import com.app.civify.service.document.ocr.SmartOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Processor for image documents (PNG, JPEG, TIFF, etc.). Uses OCR to extract
 * text from images.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ImageDocumentProcessor implements DocumentProcessor {

    private final SmartOcrService ocrService;

    @Override
    public ProcessedDocument process(MultipartFile file) {
        return process(file, null);
    }

    @Override
    public ProcessedDocument process(MultipartFile file, String languageHint) {
        long startTime = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();
        DocumentType documentType = detectDocumentType(file);

        try {
            // Read and preprocess image
            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            if (originalImage == null) {
                throw new IOException("Could not read image file");
            }

            // Preprocess image for better OCR accuracy
            BufferedImage processedImage = preprocessImage(originalImage);

            // Convert to bytes for OCR
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(processedImage, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();

            // Perform OCR
            OcrResult ocrResult = ocrService.extractText(imageBytes, "image/png", languageHint);

            long processingTime = System.currentTimeMillis() - startTime;

            if (!ocrResult.isSuccess()) {
                warnings.add("OCR processing issue: " + ocrResult.getErrorMessage());
            }

            log.info("Processed image: {}, extracted {} characters, confidence: {}",
                    file.getOriginalFilename(),
                    ocrResult.getText() != null ? ocrResult.getText().length() : 0,
                    ocrResult.getConfidence());

            return ProcessedDocument.builder()
                    .rawText(ocrResult.getText())
                    .documentType(documentType)
                    .processingMethod("OCR_" + ocrResult.getOcrProvider())
                    .pageCount(1)
                    .confidence(ocrResult.getConfidence())
                    .detectedLanguages(ocrResult.getDetectedLanguages())
                    .primaryLanguage(ocrResult.getDetectedLanguage())
                    .warnings(warnings)
                    .processingTimeMs(processingTime)
                    .originalFilename(file.getOriginalFilename())
                    .fileSizeBytes(file.getSize())
                    .build();

        } catch (IOException e) {
            log.error("Error processing image: {}", e.getMessage(), e);
            return ProcessedDocument.builder()
                    .rawText("")
                    .documentType(documentType)
                    .processingMethod("ERROR")
                    .confidence(0.0)
                    .warnings(List.of("Failed to process image: " + e.getMessage()))
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .originalFilename(file.getOriginalFilename())
                    .build();
        }
    }

    @Override
    public boolean supports(DocumentType documentType) {
        return documentType.isImage();
    }

    /**
     * Preprocess image for better OCR accuracy. Applies grayscale conversion
     * and contrast enhancement.
     */
    private BufferedImage preprocessImage(BufferedImage original) {
        // Convert to grayscale
        BufferedImage grayscale = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        Graphics2D g2d = grayscale.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        // Apply simple contrast enhancement
        return enhanceContrast(grayscale);
    }

    /**
     * Simple contrast enhancement for better OCR.
     */
    private BufferedImage enhanceContrast(BufferedImage image) {
        BufferedImage result = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                image.getType()
        );

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int gray = rgb & 0xFF;

                // Simple contrast stretch
                int enhanced = (int) ((gray - 128) * 1.2 + 128);
                enhanced = Math.max(0, Math.min(255, enhanced));

                result.setRGB(x, y, (enhanced << 16) | (enhanced << 8) | enhanced);
            }
        }

        return result;
    }
}
