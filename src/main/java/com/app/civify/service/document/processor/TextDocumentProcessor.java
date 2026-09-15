package com.app.civify.service.document.processor;

import com.app.civify.dto.document.DocumentType;
import com.app.civify.dto.document.ProcessedDocument;
import com.app.civify.service.document.DocumentProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Processor for UTF-8 plain-text and Markdown resumes supplied by remote MCP
 * clients. It avoids fabricating a PDF solely to transport already-extracted
 * resume text.
 */
@Component
@Slf4j
public class TextDocumentProcessor implements DocumentProcessor {

    @Override
    public ProcessedDocument process(MultipartFile file) {
        return process(file, null);
    }

    @Override
    public ProcessedDocument process(MultipartFile file, String languageHint) {
        long startTime = System.currentTimeMillis();
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n")
                    .replace('\r', '\n');
            if (text.startsWith("\uFEFF")) {
                text = text.substring(1);
            }

            return ProcessedDocument.builder()
                    .rawText(text.trim())
                    .documentType(DocumentType.TEXT)
                    .processingMethod("UTF8_TEXT")
                    .pageCount(1)
                    .confidence(1.0)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .originalFilename(file.getOriginalFilename())
                    .fileSizeBytes(file.getSize())
                    .build();
        } catch (IOException e) {
            log.error("Error processing text document: {}", e.getMessage(), e);
            return ProcessedDocument.builder()
                    .success(false)
                    .errorMessage("Failed to read text document")
                    .rawText("")
                    .documentType(DocumentType.TEXT)
                    .processingMethod("ERROR")
                    .confidence(0.0)
                    .warnings(List.of("Failed to read text document"))
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .originalFilename(file.getOriginalFilename())
                    .fileSizeBytes(file.getSize())
                    .build();
        }
    }

    @Override
    public boolean supports(DocumentType documentType) {
        return documentType == DocumentType.TEXT;
    }
}
