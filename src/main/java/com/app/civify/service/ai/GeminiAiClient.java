package com.app.civify.service.ai;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "geminiAiClient", url = "${gemini.base.url:https://api.openai.com}")
public interface GeminiAiClient {

    @PostMapping(value = "/v1/chat/completions", consumes = "application/json")
    String callModel(@RequestBody String requestBody);
}
