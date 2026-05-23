package com.app.civify.dto.ai;

import com.app.civify.entity.enums.AiCredentialSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard response from AI models including content and token usage metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelResponse {
    
    private String content;
    
    // Token usage metadata
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;

    /** How this call was billed; null when unset (legacy callers). */
    private AiCredentialSource credentialSource;
    
    /**
     * Create a response with just content (legacy/fallback support)
     */
    public static AiModelResponse fromContent(String content) {
        return AiModelResponse.builder()
                .content(content)
                .promptTokens(0)
                .completionTokens(0)
                .totalTokens(0)
                .build();
    }
}
