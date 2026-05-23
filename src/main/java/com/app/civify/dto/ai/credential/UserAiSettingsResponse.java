package com.app.civify.dto.ai.credential;

import com.app.civify.entity.enums.ByokMode;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class UserAiSettingsResponse {
    ByokMode byokMode;
    List<ByokFallbackEntry> fallbackChain;
    boolean canUseStrictMode;
}
