package com.app.civify.service.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "vertexAiClient", url = "${vertex.api.url:https://aiplatform.googleapis.com}")
public interface VertexAiClient {

    @PostMapping(value = "/v1/publishers/google/models/{model}:generateContent?key=${vertex.api.key}", consumes = "application/json")
    String callModel(@PathVariable String model, @RequestBody String requestBody);
}
