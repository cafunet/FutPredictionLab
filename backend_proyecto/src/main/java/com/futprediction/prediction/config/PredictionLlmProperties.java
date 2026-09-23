package com.futprediction.prediction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "prediction.llm")
public record PredictionLlmProperties(String baseUrl, boolean enabled) {
    public PredictionLlmProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8080";
        }
    }
}
