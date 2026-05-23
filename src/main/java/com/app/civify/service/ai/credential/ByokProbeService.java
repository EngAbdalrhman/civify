package com.app.civify.service.ai.credential;

import com.app.civify.dto.ai.AiModelResponse;
import com.app.civify.entity.enums.AiProviderType;
import com.app.civify.service.ai.AiProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ByokProbeService {

    private static final String PROBE_PROMPT = "Reply with exactly: OK";
    private static final Map<AiProviderType, String> DEFAULT_PROBE_MODELS = Map.of(
            AiProviderType.OPENAI, "gpt-4o-mini",
            AiProviderType.ANTHROPIC, "claude-3-5-haiku-20241022",
            AiProviderType.OPENROUTER, "openai/gpt-4o-mini",
            AiProviderType.GEMINI, "gemini-2.5-flash",
            AiProviderType.VERTEX, "gemini-2.5-flash"
    );

    private final Map<String, AiProvider> providers;

    public ByokProbeService(List<AiProvider> aiProviders) {
        this.providers = aiProviders.stream()
                .collect(Collectors.toMap(p -> p.getProviderName().toLowerCase(), Function.identity(),
                        (a, b) -> a));
    }

    public void probe(AiProviderType providerType, String apiKey, String model) {
        String providerName = providerType.toProviderName();
        AiProvider provider = providers.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Provider not available: " + providerName);
        }
        String probeModel = model != null && !model.isBlank()
                ? model
                : DEFAULT_PROBE_MODELS.getOrDefault(providerType, "gpt-4o-mini");
        AiModelResponse response = provider.callModelWithApiKey(PROBE_PROMPT, probeModel, apiKey);
        if (response.getContent() == null || response.getContent().isBlank()) {
            throw new IllegalStateException("Provider returned empty response during validation");
        }
    }
}
