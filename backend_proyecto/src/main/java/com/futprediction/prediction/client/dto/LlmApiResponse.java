package com.futprediction.prediction.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmApiResponse<T>(LocalDateTime timestamp, Integer status, String message, T data) {}
