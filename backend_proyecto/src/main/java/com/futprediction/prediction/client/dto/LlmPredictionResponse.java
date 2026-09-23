package com.futprediction.prediction.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmPredictionResponse(
        Long predictionId,
        String predictedWinner,
        Double confidence,
        Double homeWinProbability,
        Double drawProbability,
        Double awayWinProbability,
        String reasoning,
        String modelVersion,
        Integer predictedHomeScore,
        Integer predictedAwayScore) {}
