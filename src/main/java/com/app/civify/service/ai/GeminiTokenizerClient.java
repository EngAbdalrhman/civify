package com.app.civify.service.ai;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "geminiTokenizerClient",
        url = "https://generativelanguage.googleapis.com"
)
public interface GeminiTokenizerClient {

    @PostMapping("/v1beta/models/{model}:countTokens")
    String countTokens(@PathVariable String model,
                       @RequestParam("key") String apiKey,
                       @RequestBody String requestBody);
}