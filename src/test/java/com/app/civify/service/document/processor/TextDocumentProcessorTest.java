package com.app.civify.service.document.processor;

import com.app.civify.dto.document.DocumentType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TextDocumentProcessorTest {

    private final TextDocumentProcessor processor = new TextDocumentProcessor();

    @Test
    void processesUtf8ResumeTextAndNormalizesLineEndings() {
        var file = new MockMultipartFile(
                "file",
                "resume.txt",
                "text/plain",
                "\uFEFFAbdelalieem Mena\r\nJava and Spring Boot\rExperience".getBytes(StandardCharsets.UTF_8)
        );

        var result = processor.process(file);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDocumentType()).isEqualTo(DocumentType.TEXT);
        assertThat(result.getRawText()).isEqualTo("Abdelalieem Mena\nJava and Spring Boot\nExperience");
        assertThat(result.getProcessingMethod()).isEqualTo("UTF8_TEXT");
    }

    @Test
    void recognizesPlainTextAndMarkdownTypes() {
        assertThat(DocumentType.fromMimeType("text/plain")).isEqualTo(DocumentType.TEXT);
        assertThat(DocumentType.fromExtension("resume.md")).isEqualTo(DocumentType.TEXT);
    }
}
