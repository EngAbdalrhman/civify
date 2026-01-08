package com.app.civify.usecase.document;

import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.dto.document.MaskRequest;
import com.app.civify.dto.document.MaskResponse;
import com.app.civify.dto.document.ProcessedDocument;
import com.app.civify.entity.enums.TokenUsageType;
import com.app.civify.service.document.CvParsingService;
import com.app.civify.service.document.DocumentProcessorService;
import com.app.civify.service.document.masking.MaskingService;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.util.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Implementation of DocumentUseCase.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentUseCaseImpl implements DocumentUseCase {

    private final DocumentProcessorService documentProcessorService;
    private final CvParsingService cvParsingService;
    private final MaskingService maskingService;
    private final SubscriptionService subscriptionService;

    // Token costs
    private static final int TOKENS_EXTRACT = 50;
    private static final int TOKENS_MASK = 150;
    private static final int TOKENS_MASK_TEXT = 50;

    @Override
    public CvParseResponse parseCv(MultipartFile file, CvParseRequest request, Long userId) {
        log.info("Parsing CV: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

        CvParseRequest parseRequest = request != null ? request : CvParseRequest.builder().build();

        // Token charging for parse is handled inside CvParsingService
        return cvParsingService.parseCv(file, parseRequest, userId);
    }

    @Override
    public ProcessedDocument extractText(MultipartFile file, String languageHint, Long userId) {
        log.info("Extracting text from: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

        ProcessedDocument result = documentProcessorService.process(file, languageHint);

        if (result.isSuccess() && userId != null) {
            try {
                subscriptionService.chargeTokens(userId, TOKENS_EXTRACT, TokenUsageType.DOC_EXTRACT,
                        "Document Text Extraction: " + file.getOriginalFilename());
            } catch (Exception e) {
                log.error("Failed to charge tokens for extract", e);
            }
        }
        return result;
    }

    @Override
    public MaskResponse maskDocument(MultipartFile file, MaskRequest request, Long userId) {
        log.info("Masking document: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

        try {
            MaskRequest maskRequest = request != null ? request : MaskRequest.builder().build();
            MaskResponse response = maskingService.maskDocument(
                    file.getBytes(),
                    file.getContentType(),
                    maskRequest
            );

            if (response.isSuccess() && userId != null) {
                try {
                    subscriptionService.chargeTokens(userId, TOKENS_MASK, TokenUsageType.DOC_MASK,
                            "Document Masking: " + file.getOriginalFilename());
                } catch (Exception e) {
                    log.error("Failed to charge tokens for mask", e);
                }
            }
            return response;
        } catch (Exception e) {
            log.error("Error masking document", e);
            return MaskResponse.builder()
                    .success(false)
                    .errorMessage("Failed to mask document: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public MaskResponse maskText(MaskRequest request, Long userId) {
        log.info("Masking text content, length: {} chars",
                request.getText() != null ? request.getText().length() : 0);

        if (request.getText() == null || request.getText().isEmpty()) {
            return MaskResponse.builder()
                    .success(false)
                    .errorMessage("Text content is required")
                    .build();
        }

        MaskResponse response = maskingService.maskText(request.getText(), request);

        if (userId != null) {
            try {
                subscriptionService.chargeTokens(userId, TOKENS_MASK_TEXT, TokenUsageType.DOC_MASK,
                        "Text Content Masking");
            } catch (Exception e) {
                log.error("Failed to charge tokens for mask text", e);
            }
        }

        return response;
    }

    @Override
    public Set<String> getSupportedMimeTypes() {
        return documentProcessorService.getSupportedMimeTypes();
    }
}
