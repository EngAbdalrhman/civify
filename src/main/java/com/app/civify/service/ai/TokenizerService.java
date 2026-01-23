package com.app.civify.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Gemini-accurate token counting:
 * - Uses Gemini models:countTokens endpoint (same tokenizer as the model).
 * - Extracts real usage from Gemini generateContent responses (usageMetadata).
 *
 * If Gemini API key isn't configured, falls back to heuristic.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenizerService {

    private final GeminiTokenizerClient tokenizerClient;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;
    @Value("${ai.default.model:gemini-pro}")
    private String defaultModel;


    public int countTokens(String text) {
        return countTokens(defaultModel, text);
    }

    public int countTokens(String model, String text) {
        if (text == null || text.isBlank()) return 0;

        try {
            String body = buildRequest(text);
            String response = tokenizerClient.countTokens(model, apiKey, body);
            return parseTokenCount(response);
        } catch (Exception e) {
            log.warn("Gemini countTokens failed -> fallback heuristic", e);
            return heuristicTokens(text);
        }
    }
    private String buildRequest(String text) throws Exception {
        ObjectNode root = om.createObjectNode();
        ArrayNode contents = om.createArrayNode();
        ObjectNode content = om.createObjectNode();
        ArrayNode parts = om.createArrayNode();

        ObjectNode part = om.createObjectNode();
        part.put("text", text);

        parts.add(part);
        content.set("parts", parts);
        contents.add(content);
        root.set("contents", contents);

        return om.writeValueAsString(root);
    }

    private int parseTokenCount(String json) throws Exception {
        JsonNode root = om.readTree(json);
        return root.path("totalTokens").asInt();
    }

    private int heuristicTokens(String text) {
        return (int) Math.ceil(text.length() / 4.0);
    }

}
