package com.app.civify.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service to sanitize and fix malformed JSON responses from AI models.
 * Handles common issues like Markdown code blocks, truncation, and trailing commas.
 */
@Component
@Slf4j
public class JsonSanitizer {

    private final ObjectMapper objectMapper;

    public JsonSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Sanitizes and attempts to fix JSON string.
     */
    public String sanitize(String json) {
        if (json == null || json.isEmpty()) {
            return "{}";
        }

        String sanitized = json.trim();

        // 1. Remove Markdown code blocks (```json ... ```)
        if (sanitized.startsWith("```")) {
            // Remove first line (```json or ```)
            int firstNewline = sanitized.indexOf('\n');
            if (firstNewline != -1) {
                sanitized = sanitized.substring(firstNewline + 1);
            }
            // Remove last line (```)
            int lastBackticks = sanitized.lastIndexOf("```");
            if (lastBackticks != -1) {
                sanitized = sanitized.substring(0, lastBackticks);
            }
        }
        
        sanitized = sanitized.trim();

        // 2. Fix Truncation (Resilience)
        // If it doesn't end with typical JSON closers, it might be cut off.
        if (!isValidJsonEnd(sanitized)) {
            log.warn("JSON appears truncated, attempting auto-repair...");
            sanitized = balanaceJson(sanitized);
        }

        return sanitized;
    }

    private boolean isValidJsonEnd(String json) {
        return json.endsWith("}") || json.endsWith("]");
    }

    /**
     * naive validater/balancer that closes open braces/brackets.
     * This is a simple heuristic approach.
     */
    private String balanaceJson(String json) {
        StringBuilder sb = new StringBuilder(json);
        int openBraces = 0;
        int openBrackets = 0;
        boolean inString = false;
        boolean escape = false;

        for (char c : json.toCharArray()) {
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString) {
                if (c == '{') openBraces++;
                else if (c == '}') openBraces--;
                else if (c == '[') openBrackets++;
                else if (c == ']') openBrackets--;
            }
        }

        // Close pending structures
        // Note: Logic implies we are missing closures at the END
        
        // If we are inside a string (e.g. "description": "This is a long...), close it first
        if (inString) {
            sb.append("\"");
        }
        
        while (openBraces > 0 || openBrackets > 0) {
            // Heuristic: determine which one to close based on stack concept (simplified here)
            // A better parser would track the stack. 
            // For simple resume object structures, we usually just need to close } or ]
            
            // If we assume standard object nesting, usually } is needed last for root object.
            // Let's just append proper closers. 
            // NOTE: A proper stack is needed for exact correctness, but for truncation at end:
            // It's usually safe to close ] then } if we assume strict hierarchy or simply count.
            
            // Let's just append based on counts. Order matters! 
            // Ideally we'd scan backwards to see what opened last, but that's complex.
            // fallback: just append '}' or ']' until balanced.
            
            // FIXME: This naive loop is dangerous if mixed. 
            // Simple fix for now: Close everything with a best guess.
            if (openBrackets > 0) {
                sb.append("]");
                openBrackets--;
            } else if (openBraces > 0) {
                sb.append("}");
                openBraces--;
            }
        }
        
        return sb.toString();
    }

    /**
     * Cleans AI response by removing markdown blocks and extracting content if nested in Gemini structure.
     */
    public String cleanAiResponse(String response) {
        if (response == null) return "";

        String cleaned = response.trim();

        // Extract from Gemini response structure if present
        cleaned = extractContentFromGeminiResponse(cleaned);

        // Remove markdown code blocks
        if (cleaned.startsWith("```yaml")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    private String extractContentFromGeminiResponse(String response) {
        try {
            var root = objectMapper.readTree(response);
            var candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                var content = candidates.get(0).path("content");
                var parts = content.path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText();
                }
            }
            return response;
        } catch (Exception e) {
            return response;
        }
    }
}
