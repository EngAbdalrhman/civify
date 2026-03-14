package com.app.civify.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to sanitize and fix malformed JSON/YAML responses from AI models.
 * Handles common issues like Markdown code blocks, truncation, trailing commas,
 * and unquoted YAML string values containing special characters.
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
            sanitized = balanceJson(sanitized);
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
    private String balanceJson(String json) {
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
            // Close brackets before braces (arrays are typically nested inside objects)
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

        cleaned = cleaned.trim();

        // Repair YAML: auto-quote unquoted values containing colons
        if (looksLikeYaml(cleaned)) {
            cleaned = repairYaml(cleaned);
        }

        return cleaned;
    }

    /**
     * Check if the content looks like YAML (not JSON).
     */
    private boolean looksLikeYaml(String content) {
        if (content.isEmpty()) return false;
        char first = content.charAt(0);
        // JSON starts with { or [, YAML doesn't
        return first != '{' && first != '[';
    }

    // Pattern: line with leading whitespace, a YAML key, colon, space, then an UNQUOTED value
    // Group 1 = indentation + key + ": "
    // Group 2 = the value part (unquoted)
    private static final Pattern YAML_KV_LINE = Pattern.compile(
            "^(\\s*[\\w][\\w\\s.-]*:\\s)(.+)$");

    /**
     * Repairs malformed YAML by quoting unquoted string values that contain
     * special characters (colons, etc.) which break YAML parsers.
     */
    public String repairYaml(String yaml) {
        if (yaml == null || yaml.isEmpty()) return yaml;

        String[] lines = yaml.split("\n");
        StringBuilder repaired = new StringBuilder(yaml.length() + 256);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // Strip trailing \r for consistent processing
            String trimmedLine = line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
            String stripped = trimmedLine.stripLeading();

            // Skip empty lines, comments, list-only markers, block scalar indicators
            if (stripped.isEmpty() || stripped.startsWith("#")
                    || stripped.equals("-") || stripped.equals("|-") || stripped.equals("|") || stripped.equals(">")) {
                repaired.append(trimmedLine);
                if (i < lines.length - 1) repaired.append("\n");
                continue;
            }

            Matcher m = YAML_KV_LINE.matcher(trimmedLine);
            if (m.matches()) {
                String prefix = m.group(1); // "  key: "
                String value = m.group(2).trim();  // the value part

                // Handle list-item keys like "- id: value"
                if (stripped.startsWith("- ")) {
                    // Re-match after the "- " to capture the actual key: value
                    String afterDash = stripped.substring(2);
                    Matcher dashM = YAML_KV_LINE.matcher(afterDash);
                    if (dashM.matches()) {
                        String indent = trimmedLine.substring(0, trimmedLine.length() - stripped.length());
                        prefix = indent + "- " + dashM.group(1);
                        value = dashM.group(2).trim();
                    }
                }

                boolean needsQuoting = shouldQuoteValue(value);

                if (needsQuoting) {
                    // Escape existing double quotes inside the value
                    String escaped = value.replace("\\\"", "\"")  // unescape first
                                         .replace("\"", "\\\"");   // then escape
                    repaired.append(prefix).append("\"").append(escaped).append("\"");
                    log.debug("YAML repair: quoted value on line {}: {}", i + 1, value);
                } else {
                    repaired.append(trimmedLine);
                }
            } else {
                repaired.append(trimmedLine);
            }

            if (i < lines.length - 1) repaired.append("\n");
        }

        return repaired.toString();
    }

    /**
     * Determines if a YAML value needs to be wrapped in quotes.
     * Values that are already quoted, booleans, numbers, nulls, lists, or
     * block scalars are left alone. Everything else with a colon gets quoted.
     */
    private boolean shouldQuoteValue(String value) {
        if (value.isEmpty()) return false;

        // Already quoted (single or double)
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return false;
        }

        // Block scalar indicators
        if (value.equals("|") || value.equals(">") || value.equals("|-") || value.equals(">-")) {
            return false;
        }

        // List/object start
        if (value.startsWith("[") || value.startsWith("{")) {
            return false;
        }

        // Booleans, null
        String lower = value.toLowerCase();
        if (lower.equals("true") || lower.equals("false") || lower.equals("null")
                || lower.equals("yes") || lower.equals("no")) {
            return false;
        }

        // Pure numeric
        try {
            Double.parseDouble(value);
            return false;
        } catch (NumberFormatException ignored) {
        }

        // Contains colon → definitely needs quoting
        if (value.contains(":")) {
            return true;
        }

        // Contains other YAML-special chars that could cause issues
        if (value.contains("#") || value.contains("&") || value.contains("*")
                || value.contains("!") || value.contains("%") || value.contains("@")
                || value.contains("`")) {
            return true;
        }

        return false;
    }

    public <T> T cleanAiResponse(String jsonResponse, Class<T> targetType) {
        try {
            // Use the string overload to standardise extraction and markdown stripping
            String cleanJson = cleanAiResponse(jsonResponse);
            
            // Check for potential truncation before parsing if possible, or parse first
            // Parse first to check structural truncation
            T result = objectMapper.readValue(cleanJson, targetType);
            
            if (isTruncated(result)) {
                log.warn("AI response appears truncated. Missing critical fields or empty lists.");
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to clean AI response, attempting raw parse: {}", e.getMessage());
            try {
                return objectMapper.readValue(jsonResponse, targetType);
            } catch (Exception ex) {
                log.error("Failed to parse AI response: {}", ex.getMessage());
                throw new RuntimeException("Failed to parse AI response", ex);
            }
        }
    }

    public boolean isTruncated(Object result) {
        if (result == null) return true;
        
        try {
            JsonNode node = objectMapper.valueToTree(result);
            
            // Check for personalInfo (required for CVs)
            if (node.has("personalInfo")) {
                JsonNode personalInfo = node.get("personalInfo");
                if (personalInfo.isNull() || personalInfo.isEmpty()) {
                    return true;
                }
            }
            
            // Check for generic sections array (ResumeData structure)
            if (node.has("sections")) {
                 JsonNode sections = node.get("sections");
                 if (sections.isArray()) {
                     // If sections array exists but is empty, it's suspicious for a CV
                     if (sections.isEmpty()) return true;
                     
                     // If sections array has only 1 item (e.g. just education), it MIGHT be truncated
                     // but we can't be sure.
                 }
            } else if (node.has("basics")) { // JSON Resume schema fallback
                 JsonNode basics = node.get("basics");
                 if (basics.isNull() || basics.isEmpty()) return true;
            }

            // Check specific arrays that usually have content (for JSON Resume schema)
            String[] suspiciousEmptyArrays = {"workExperience", "education", "skills", "projects"};
            for (String key : suspiciousEmptyArrays) {
                if (node.has(key)) {
                     JsonNode array = node.get(key);
                     if (array.isArray() && array.isEmpty()) {
                         log.debug("Suspicious empty array: {}", key);
                     }
                }
            }
            
            return false;
        } catch (Exception e) {
            log.warn("Error checking for truncation", e);
            return false;
        }
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
