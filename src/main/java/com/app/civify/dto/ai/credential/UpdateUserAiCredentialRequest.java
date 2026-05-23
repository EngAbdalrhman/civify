package com.app.civify.dto.ai.credential;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserAiCredentialRequest {

    @Size(max = 64)
    private String label;

    @Size(max = 128)
    private String defaultModel;
}
