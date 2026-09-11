package com.tripping.trippingserver.external.groq;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GroqApiProperties {

    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public GroqApiProperties(
            @Value("${groq.api-key}") String apiKey,
            @Value("${groq.base-url}") String baseUrl,
            @Value("${groq.model}") String model
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }
}