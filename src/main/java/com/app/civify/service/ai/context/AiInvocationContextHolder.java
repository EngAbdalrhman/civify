package com.app.civify.service.ai.context;

import com.app.civify.entity.enums.AiCredentialSource;

/**
 * Thread-local metadata for the last AI invocation (billing / logging).
 */
public final class AiInvocationContextHolder {

    private static final ThreadLocal<InvocationMeta> CURRENT = new ThreadLocal<>();

    private AiInvocationContextHolder() {}

    public record InvocationMeta(AiCredentialSource source, boolean usedPlatformFallback) {}

    public static void set(InvocationMeta meta) {
        CURRENT.set(meta);
    }

    public static InvocationMeta get() {
        return CURRENT.get();
    }

    public static boolean usedByok() {
        InvocationMeta meta = CURRENT.get();
        return meta != null && meta.source() == AiCredentialSource.USER_BYOK;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
