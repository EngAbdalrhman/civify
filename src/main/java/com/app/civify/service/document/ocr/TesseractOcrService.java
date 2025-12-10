package com.app.civify.service.document.ocr;

import com.app.civify.dto.document.OcrResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * OCR service using Tesseract (open source).
 * Fallback OCR provider when Gemini Vision is unavailable.
 */
@Service
@Slf4j
@Order(2)
public class TesseractOcrService implements OcrService {
    
    private Tesseract tesseract;
    private boolean available = false;
    
    @Value("${ocr.tesseract.datapath:}")
    private String tessDataPath;
    
    @Value("${ocr.tesseract.enabled:true}")
    private boolean enabled;
    
    @Value("${ocr.tesseract.default-language:eng+ara}")
    private String defaultLanguage;
    
    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("Tesseract OCR is disabled");
            return;
        }
        
        try {
            tesseract = new Tesseract();
            
            // Set data path
            if (tessDataPath != null && !tessDataPath.isEmpty()) {
                tesseract.setDatapath(tessDataPath);
            } else {
                // Try common paths
                String[] commonPaths = {
                    "/usr/share/tesseract-ocr/4.00/tessdata",
                    "/usr/share/tesseract-ocr/5/tessdata",
                    "/usr/local/share/tessdata",
                    "C:\\Program Files\\Tesseract-OCR\\tessdata"
                };
                
                for (String path : commonPaths) {
                    if (new File(path).exists()) {
                        tesseract.setDatapath(path);
                        break;
                    }
                }
            }
            
            // Configure for best accuracy
            tesseract.setLanguage(defaultLanguage);
            tesseract.setPageSegMode(1); // Automatic page segmentation with OSD
            tesseract.setOcrEngineMode(1); // LSTM neural network mode
            
            available = true;
            log.info("Tesseract OCR initialized successfully with language: {}", defaultLanguage);
            
        } catch (Exception e) {
            log.warn("Tesseract OCR initialization failed: {}. OCR fallback will not be available.", e.getMessage());
            available = false;
        }
    }
    
    @Override
    public OcrResult extractText(byte[] imageData, String mimeType) {
        return extractText(imageData, mimeType, null);
    }
    
    @Override
    public OcrResult extractText(byte[] imageData, String mimeType, String languageHint) {
        long startTime = System.currentTimeMillis();
        
        if (!isAvailable()) {
            return OcrResult.error("Tesseract OCR service is not available");
        }
        
        try {
            // Set language based on hint
            String language = mapLanguageHint(languageHint);
            tesseract.setLanguage(language);
            
            // Convert bytes to BufferedImage
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            
            if (image == null) {
                return OcrResult.error("Could not read image data");
            }
            
            // Perform OCR
            String extractedText = tesseract.doOCR(image);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            log.info("Tesseract OCR completed in {}ms, extracted {} characters", 
                    processingTime, extractedText.length());
            
            return OcrResult.builder()
                    .text(extractedText.trim())
                    .confidence(0.75) // Tesseract typically has lower accuracy than cloud services
                    .success(true)
                    .ocrProvider("TESSERACT")
                    .processingTimeMs(processingTime)
                    .detectedLanguage(languageHint != null ? languageHint : "unknown")
                    .build();
                    
        } catch (TesseractException e) {
            log.error("Tesseract OCR failed: {}", e.getMessage(), e);
            return OcrResult.builder()
                    .text("")
                    .confidence(0.0)
                    .success(false)
                    .errorMessage("Tesseract OCR failed: " + e.getMessage())
                    .ocrProvider("TESSERACT")
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            log.error("Tesseract OCR error: {}", e.getMessage(), e);
            return OcrResult.error("Tesseract OCR error: " + e.getMessage());
        }
    }
    
    @Override
    public boolean isAvailable() {
        return enabled && available && tesseract != null;
    }
    
    @Override
    public String getProviderName() {
        return "TESSERACT";
    }
    
    @Override
    public int getPriority() {
        return 2; // Second priority (fallback)
    }
    
    /**
     * Map language hint to Tesseract language codes.
     */
    private String mapLanguageHint(String languageHint) {
        if (languageHint == null || languageHint.isEmpty()) {
            return defaultLanguage;
        }
        
        return switch (languageHint.toLowerCase()) {
            case "ar", "arabic" -> "ara+eng";
            case "en", "english" -> "eng";
            case "fr", "french" -> "fra+eng";
            case "de", "german" -> "deu+eng";
            case "es", "spanish" -> "spa+eng";
            default -> defaultLanguage;
        };
    }
}

