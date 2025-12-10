package com.app.civify.service.document.processor;

import com.app.civify.dto.document.DocumentType;
import com.app.civify.dto.document.ProcessedDocument;
import com.app.civify.service.document.DocumentProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Processor for Microsoft Word DOCX documents.
 */
@Component
@Slf4j
public class DocxDocumentProcessor implements DocumentProcessor {
    
    @Override
    public ProcessedDocument process(MultipartFile file) {
        return process(file, null);
    }
    
    @Override
    public ProcessedDocument process(MultipartFile file, String languageHint) {
        long startTime = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();
        
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            StringBuilder textBuilder = new StringBuilder();
            
            // Extract paragraphs
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    textBuilder.append(text).append("\n");
                }
            }
            
            // Extract tables
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    StringBuilder rowText = new StringBuilder();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.trim().isEmpty()) {
                            if (rowText.length() > 0) {
                                rowText.append(" | ");
                            }
                            rowText.append(cellText.trim());
                        }
                    }
                    if (rowText.length() > 0) {
                        textBuilder.append(rowText).append("\n");
                    }
                }
                textBuilder.append("\n");
            }
            
            // Extract headers
            for (XWPFHeader header : document.getHeaderList()) {
                for (XWPFParagraph paragraph : header.getParagraphs()) {
                    String text = paragraph.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        textBuilder.insert(0, text + "\n");
                    }
                }
            }
            
            // Extract footers
            for (XWPFFooter footer : document.getFooterList()) {
                for (XWPFParagraph paragraph : footer.getParagraphs()) {
                    String text = paragraph.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        textBuilder.append(text).append("\n");
                    }
                }
            }
            
            String extractedText = textBuilder.toString().trim();
            long processingTime = System.currentTimeMillis() - startTime;
            
            log.info("Processed DOCX: {}, extracted {} characters", 
                    file.getOriginalFilename(), extractedText.length());
            
            return ProcessedDocument.builder()
                    .rawText(extractedText)
                    .documentType(DocumentType.DOCX)
                    .processingMethod("APACHE_POI")
                    .pageCount(document.getProperties().getExtendedProperties()
                            .getUnderlyingProperties().getPages())
                    .confidence(1.0)
                    .warnings(warnings)
                    .processingTimeMs(processingTime)
                    .originalFilename(file.getOriginalFilename())
                    .fileSizeBytes(file.getSize())
                    .build();
                    
        } catch (IOException e) {
            log.error("Error processing DOCX: {}", e.getMessage(), e);
            return ProcessedDocument.builder()
                    .rawText("")
                    .documentType(DocumentType.DOCX)
                    .processingMethod("ERROR")
                    .confidence(0.0)
                    .warnings(List.of("Failed to process DOCX: " + e.getMessage()))
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .originalFilename(file.getOriginalFilename())
                    .build();
        }
    }
    
    @Override
    public boolean supports(DocumentType documentType) {
        return documentType.isWord();
    }
}

