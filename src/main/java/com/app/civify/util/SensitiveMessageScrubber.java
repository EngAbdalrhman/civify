package com.app.civify.util;

import java.util.regex.Pattern;

public final class SensitiveMessageScrubber {

    private static final Pattern BEARER = Pattern.compile("Bearer\\s+\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern API_KEY_PARAM = Pattern.compile("(key=)[^&\\s\"']+", Pattern.CASE_INSENSITIVE);
    private static final Pattern SK_LIKE = Pattern.compile("sk-[A-Za-z0-9_-]{8,}", Pattern.CASE_INSENSITIVE);

    private SensitiveMessageScrubber() {}

    public static String scrub(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }
        String scrubbed = BEARER.matcher(message).replaceAll("Bearer [REDACTED]");
        scrubbed = API_KEY_PARAM.matcher(scrubbed).replaceAll("$1[REDACTED]");
        scrubbed = SK_LIKE.matcher(scrubbed).replaceAll("sk-[REDACTED]");
        return scrubbed;
    }
}
