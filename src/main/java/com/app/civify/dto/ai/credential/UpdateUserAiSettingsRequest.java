package com.app.civify.dto.ai.credential;

import com.app.civify.entity.enums.ByokMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserAiSettingsRequest {

    @NotNull
    private ByokMode byokMode;

    private List<ByokFallbackEntry> fallbackChain;
}
