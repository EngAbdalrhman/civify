package com.app.civify.service.ai.client;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ByokRestClientFactory {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    private final Map<String, RestClient> clientsByBaseUrl = new ConcurrentHashMap<>();

    public RestClient forBaseUrl(String baseUrl) {
        return clientsByBaseUrl.computeIfAbsent(baseUrl, this::buildClient);
    }

    public RestClient ephemeral() {
        return buildClient("");
    }

    private RestClient buildClient(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
        requestFactory.setReadTimeout((int) READ_TIMEOUT.toMillis());

        RestClient.Builder builder = RestClient.builder().requestFactory(requestFactory);
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }
}
