package com.tripping.trippingserver.external.tourism;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TourismApiProperties {

    @Value("${tourism.api-key}")
    private String apiKey;

    @Value("${tourism.base-url}")
    private String baseUrl;

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}