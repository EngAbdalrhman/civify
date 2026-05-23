package com.app.civify.service.ai;

import com.app.civify.config.AiByokProperties;
import com.app.civify.dto.ai.AiModelResponse;
import com.app.civify.entity.enums.AiCredentialSource;
import com.app.civify.entity.enums.ByokMode;
import com.app.civify.entity.enums.TokenUsageType;
import com.app.civify.exception.AiByokExhaustedException;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.ai.context.AiInvocationContextHolder;
import com.app.civify.service.ai.credential.AiCredentialResolver;
import com.app.civify.service.ai.credential.AiCredentialService;
import com.app.civify.service.ai.credential.ResolvedAiCall;
import com.app.civify.service.ai.usage.AiCredentialUsageRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI service with multi-provider support, automatic failover, and optional BYOK routing.
 */
@Service
@Slf4j
public class AiServiceImpl implements AiService {

    private final Map<String, AiProvider> providers;
    private final AiByokProperties byokProperties;
    private final AiCredentialResolver credentialResolver;
    private final AiCredentialUsageRecorder usageRecorder;
    private final AiCredentialService credentialService;

    @Value("${ai.fallback.provider:gemini}")
    private String fallbackProviderName;

    @Value("${ai.fallback.model:gemini-2.5-flash}")
    private String fallbackModel;

    public AiServiceImpl(
            List<AiProvider> aiProviders,
            AiByokProperties byokProperties,
            AiCredentialResolver credentialResolver,
            AiCredentialUsageRecorder usageRecorder,
            AiCredentialService credentialService) {
        this.providers = aiProviders.stream()
                .collect(Collectors.toMap(
                        p -> p.getProviderName().toLowerCase(),
                        Function.identity(),
                        (existing, replacement) -> existing,
                        ConcurrentHashMap::new));
        this.byokProperties = byokProperties;
        this.credentialResolver = credentialResolver;
        this.usageRecorder = usageRecorder;
        this.credentialService = credentialService;
        log.info("AI Service initialized with providers: {}", providers.keySet());
    }

    @Override
    public AiModelResponse callModel(String prompt, String provider, String model) {
        if (!byokProperties.isEnabled()) {
            return callPlatformPath(prompt, provider, model);
        }
        Optional<Long> userId = resolveAuthenticatedUserId();
        if (userId.isEmpty()) {
            return callPlatformPath(prompt, provider, model);
        }
        return callWithResolver(userId.get(), TokenUsageType.OTHER, prompt, provider, model, null);
    }

    private AiModelResponse callWithResolver(
            Long userId,
            TokenUsageType usageType,
            String prompt,
            String provider,
            String model,
            Long organizationId) {

        List<ResolvedAiCall> chain = credentialResolver.resolve(userId, usageType, provider, model);
        ByokMode mode = credentialService.getOrCreateSettings(userId).getByokMode();

        if (chain.isEmpty() && mode == ByokMode.STRICT) {
            throw new AiByokExhaustedException(mode, List.of(), false);
        }

        List<AiByokExhaustedException.ByokAttemptFailure> failures = new ArrayList<>();
        RuntimeException lastError = null;

        try {
            for (ResolvedAiCall call : chain) {
                long start = System.currentTimeMillis();
                try {
                    AiModelResponse response = executeResolvedCall(prompt, call);
                    int latency = (int) (System.currentTimeMillis() - start);
                    recordUsage(userId, organizationId, call, usageType, response, latency, true, null, null);
                    AiInvocationContextHolder.set(new AiInvocationContextHolder.InvocationMeta(
                            call.getSource(),
                            call.isPlatformPath() && !failures.isEmpty()));
                    if (!call.isPlatformPath() && !failures.isEmpty()) {
                        log.info("BYOK chain succeeded after {} failed attempt(s) on provider {}",
                                failures.size(), call.getProviderName());
                    }
                    return response;
                } catch (RuntimeException ex) {
                    lastError = ex;
                    int latency = (int) (System.currentTimeMillis() - start);
                    String errorCode = classifyError(ex);
                    recordUsage(userId, organizationId, call, usageType, null, latency, false, errorCode, ex.getMessage());
                    if (!call.isPlatformPath()) {
                        failures.add(new AiByokExhaustedException.ByokAttemptFailure(
                                call.getCredentialId() != null ? call.getCredentialId().toString() : null,
                                call.getProviderName(),
                                call.getModel(),
                                errorCode));
                        log.warn("BYOK attempt failed provider={} model={}: {}",
                                call.getProviderName(), call.getModel(), ex.getMessage());
                        continue;
                    }
                    throw ex;
                }
            }

            if (mode == ByokMode.STRICT && !failures.isEmpty()) {
                throw new AiByokExhaustedException(mode, failures, false);
            }
            if (lastError != null) {
                throw lastError;
            }
            throw new RuntimeException("AI service unavailable — no providers in resolution chain");
        } finally {
            // Context cleared by caller after billing if needed; set remains until next call overwrites
        }
    }

    /**
     * Legacy platform-only path — identical behavior to pre-BYOK implementation.
     */
    private AiModelResponse callPlatformPath(String prompt, String provider, String model) {
        try {
            AiInvocationContextHolder.set(new AiInvocationContextHolder.InvocationMeta(
                    AiCredentialSource.PLATFORM, false));
            return callProvider(provider, model, prompt, null);
        } catch (Exception primaryEx) {
            log.warn("Primary provider '{}' failed: {}. Attempting fallback to '{}'...",
                    provider, primaryEx.getMessage(), fallbackProviderName);

            if (fallbackProviderName.equalsIgnoreCase(provider)) {
                throw new RuntimeException(
                        "AI service unavailable. Provider '" + provider + "' failed: " + primaryEx.getMessage(),
                        primaryEx);
            }

            try {
                AiModelResponse fallbackResponse = callProvider(fallbackProviderName, fallbackModel, prompt, null);
                AiInvocationContextHolder.set(new AiInvocationContextHolder.InvocationMeta(
                        AiCredentialSource.PLATFORM, true));
                return fallbackResponse;
            } catch (Exception fallbackEx) {
                throw new RuntimeException(
                        "AI service unavailable. Primary ('" + provider + "'): " + primaryEx.getMessage()
                                + ". Fallback ('" + fallbackProviderName + "'): " + fallbackEx.getMessage(),
                        fallbackEx);
            }
        }
    }

    private AiModelResponse executeResolvedCall(String prompt, ResolvedAiCall call) {
        if (call.isPlatformPath()) {
            return callProvider(call.getProviderName(), call.getModel(), prompt, null);
        }
        return callProvider(call.getProviderName(), call.getModel(), prompt, call.getApiKeyOverride());
    }

    private AiModelResponse callProvider(String providerName, String model, String prompt, String apiKeyOverride) {
        AiProvider aiProvider = providers.get(providerName.toLowerCase());
        if (aiProvider == null) {
            throw new IllegalArgumentException("Unsupported AI provider: " + providerName
                    + ". Available providers: " + providers.keySet());
        }

        if (apiKeyOverride == null && !aiProvider.isAvailable()) {
            throw new IllegalStateException(
                    "AI provider '" + providerName + "' is not configured properly (missing API key/config).");
        }

        try {
            if (apiKeyOverride != null && !apiKeyOverride.isBlank()) {
                return aiProvider.callModelWithApiKey(prompt, model, apiKeyOverride);
            }
            AiModelResponse response = aiProvider.callModel(prompt, model);
            log.debug("AI model call successful - Provider: {}, Tokens: {}", providerName, response.getTotalTokens());
            return response;
        } catch (Exception e) {
            log.error("Error calling AI model (Provider: {}, Model: {}): {}", providerName, model, e.getMessage());
            throw new RuntimeException("Failed to call AI model: " + e.getMessage(), e);
        }
    }

    private void recordUsage(
            Long userId,
            Long organizationId,
            ResolvedAiCall call,
            TokenUsageType usageType,
            AiModelResponse response,
            int latencyMs,
            boolean success,
            String errorCode,
            String errorMessage) {
        int promptTokens = response != null ? response.getPromptTokens() : 0;
        int completionTokens = response != null ? response.getCompletionTokens() : 0;
        usageRecorder.record(
                userId,
                organizationId,
                call.getCredentialId(),
                call.getSource(),
                call.getProviderName(),
                call.getModel(),
                usageType,
                promptTokens,
                completionTokens,
                latencyMs,
                success,
                errorCode,
                errorMessage);
    }

    private static String classifyError(Throwable ex) {
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (msg.contains("401") || msg.contains("403") || msg.contains("auth") || msg.contains("api key")) {
            return "AUTH_FAILED";
        }
        if (msg.contains("429") || msg.contains("rate")) {
            return "RATE_LIMITED";
        }
        if (msg.contains("insufficient") || msg.contains("quota") || msg.contains("billing")) {
            return "INSUFFICIENT_FUNDS";
        }
        return "PROVIDER_ERROR";
    }

    private Optional<Long> resolveAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return Optional.ofNullable(userPrincipal.getId());
        }
        return Optional.empty();
    }
}
