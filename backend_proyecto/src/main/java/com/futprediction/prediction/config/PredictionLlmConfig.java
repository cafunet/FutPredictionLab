package com.futprediction.prediction.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PredictionLlmProperties.class)
public class PredictionLlmConfig {

    @Bean
    RestClient predictionRestClient() {
        return RestClient.builder().build();
    }
}
