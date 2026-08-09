package com.example.agenticai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    /** Read from GEMINI_API_KEY environment variable or application.properties */
    private String apiKey;
    private String model = "gemini-2.0-flash";
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
