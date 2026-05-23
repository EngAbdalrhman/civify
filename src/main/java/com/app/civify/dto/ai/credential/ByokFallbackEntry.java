package com.app.civify.dto.ai.credential;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ByokFallbackEntry {
    private UUID credentialId;
    private String modelOverride;
}
