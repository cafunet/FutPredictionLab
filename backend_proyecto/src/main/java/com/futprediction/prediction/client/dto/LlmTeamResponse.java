package com.futprediction.prediction.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmTeamResponse(Long id, String name, String country) {}
