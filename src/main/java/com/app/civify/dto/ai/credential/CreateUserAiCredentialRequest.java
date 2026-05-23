package com.app.civify.dto.ai.credential;

import com.app.civify.entity.enums.AiProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserAiCredentialRequest {

    @NotNull
    private AiProviderType provider;

    @NotBlank
    @Size(max = 64)
    private String label;

    @NotBlank
    private String apiKey;

    @Size(max = 128)
    private String defaultModel;
}
