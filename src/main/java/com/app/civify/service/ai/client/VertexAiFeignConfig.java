package com.app.civify.service.ai.client;

import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

public class VertexAiFeignConfig {

    @Bean
    public Retryer retryer() {
        // Start waiting 1s, max wait 2s, max 3 attempts
        return new Retryer.Default(1000, 2000, 3);
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new ErrorDecoder.Default() {
            @Override
            public Exception decode(String methodKey, feign.Response response) {
                if (response.status() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                    return new RetryableException(
                        response.status(),
                        "Too Many Requests (429) from Vertex AI",
                        response.request().httpMethod(),
                        (java.util.Date) null,
                        response.request()
                    );
                }
                return super.decode(methodKey, response);
            }
        };
    }
}
