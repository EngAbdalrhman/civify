package com.app.civify.service.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "geminiAiClient", url = "${gemini.base.url:https://generativelanguage.googleapis.com}", configuration = GeminiAiFeignConfig.class)
public interface GeminiAiClient {

    @PostMapping(value = "/v1beta/models/{model}:generateContent?key=${gemini.api.key}", consumes = "application/json")
    String callModel(@PathVariable String model, @RequestBody String requestBody);
}
