package com.app.civify.service.document.ocr;

import com.app.civify.dto.document.OcrResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Smart OCR service that orchestrates multiple OCR providers with fallback logic.
 * Tries providers in priority order and falls back to next if one fails.
 */
@Service
@Slf4j
public class SmartOcrService implements OcrService {
    
    private final List<OcrService> ocrProviders;
    
    public SmartOcrService(List<OcrService> ocrProviders) {
        // Sort by priority and exclude this service to avoid circular reference
        this.ocrProviders = ocrProviders.stream()
                .filter(provider -> !(provider instanceof SmartOcrService))
                .sorted(Comparator.comparingInt(OcrService::getPriority))
                .toList();
        
        log.info("SmartOcrService initialized with {} providers: {}", 
                this.ocrProviders.size(),
                this.ocrProviders.stream()
                        .map(OcrService::getProviderName)
                        .toList());
    }
    
    @Override
    public OcrResult extractText(byte[] imageData, String mimeType) {
        return extractText(imageData, mimeType, null);
    }
    
    @Override
    public OcrResult extractText(byte[] imageData, String mimeType, String languageHint) {
        OcrResult lastResult = null;
        StringBuilder allErrors = new StringBuilder();
        
        for (OcrService provider : ocrProviders) {
            if (!provider.isAvailable()) {
                log.debug("OCR provider {} is not available, skipping", provider.getProviderName());
                continue;
            }
            
            try {
                log.info("Attempting OCR with provider: {}", provider.getProviderName());
                OcrResult result = provider.extractText(imageData, mimeType, languageHint);
                
                if (result.isSuccess() && result.hasText()) {
                    log.info("OCR successful with provider: {}, confidence: {}", 
                            provider.getProviderName(), result.getConfidence());
                    return result;
                }
                
                lastResult = result;
                String errorMsg = result.getErrorMessage() != null ? result.getErrorMessage() : "No text extracted";
                allErrors.append(provider.getProviderName()).append(": ").append(errorMsg).append("; ");
                log.warn("OCR provider {} did not return satisfactory results: {}", 
                        provider.getProviderName(), errorMsg);
                        
            } catch (Exception e) {
                log.error("OCR provider {} threw exception: {}", provider.getProviderName(), e.getMessage());
                allErrors.append(provider.getProviderName()).append(": ").append(e.getMessage()).append("; ");
            }
        }
        
        // All providers failed or returned empty results
        if (lastResult != null && lastResult.hasText()) {
            // Return last result even if confidence is low
            return lastResult;
        }
        
        return OcrResult.builder()
                .text("")
                .confidence(0.0)
                .success(false)
                .errorMessage("All OCR providers failed: " + allErrors)
                .ocrProvider("SMART_OCR")
                .build();
    }
    
    @Override
    public boolean isAvailable() {
        return ocrProviders.stream().anyMatch(OcrService::isAvailable);
    }
    
    @Override
    public String getProviderName() {
        return "SMART_OCR";
    }
    
    @Override
    public int getPriority() {
        return 0; // Should not be used in provider list
    }
    
    /**
     * Get list of available OCR providers.
     */
    public List<String> getAvailableProviders() {
        return ocrProviders.stream()
                .filter(OcrService::isAvailable)
                .map(OcrService::getProviderName)
                .toList();
    }
}

