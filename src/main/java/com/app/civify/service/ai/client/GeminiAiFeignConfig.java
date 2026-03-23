package com.app.civify.service.ai.client;

import feign.Request;
import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GeminiAiFeignConfig {

    @Bean
    public Retryer geminiRetryer() {
        // Jitter retryer: base 1s, max 5s, 3 attempts
        // Each retry adds ±30% randomization to prevent burst collisions
        return new Retryer() {
            private final long basePeriod = 1000;
            private final long maxPeriod = 5000;
            private final int maxAttempts = 3;
            private int attempt = 1;

            @Override
            public void continueOrPropagate(RetryableException e) {
                if (attempt >= maxAttempts) {
                    throw e;
                }
                long delay = Math.min(basePeriod * (1L << (attempt - 1)), maxPeriod);
                // Add ±30% jitter
                long jitter = (long) (delay * 0.3 * (ThreadLocalRandom.current().nextDouble() * 2 - 1));
                long actualDelay = Math.max(500, delay + jitter);

                log.warn("Gemini AI retry attempt {}/{} after {}ms (jittered from {}ms). Error: {}",
                        attempt + 1, maxAttempts, actualDelay, delay, e.getMessage());
                attempt++;

                try {
                    Thread.sleep(actualDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }

            @Override
            public Retryer clone() {
                return new GeminiAiFeignConfig().geminiRetryer();
            }
        };
    }

    @Bean
    public ErrorDecoder geminiErrorDecoder() {
        return new ErrorDecoder.Default() {
            @Override
            public Exception decode(String methodKey, feign.Response response) {
                int status = response.status();

                // Retry on 429 (Too Many Requests) and 503 (Service Unavailable)
                if (status == HttpStatus.TOO_MANY_REQUESTS.value()
                        || status == HttpStatus.SERVICE_UNAVAILABLE.value()) {

                    String errorType = status == 429 ? "Too Many Requests (429)" : "Service Unavailable (503)";
                    log.warn("Gemini AI returned {} — marking as retryable", errorType);

                    return new RetryableException(
                            status,
                            errorType + " from Gemini AI",
                            response.request().httpMethod(),
                            (Long) null,
                            response.request()
                    );
                }
                return super.decode(methodKey, response);
            }
        };
    }

    @Bean
    public Request.Options geminiOptions() {
        // Connect timeout: 10s, Read timeout: 120s
        return new Request.Options(
                10, TimeUnit.SECONDS,
                120, TimeUnit.SECONDS,
                true
        );
    }
}
