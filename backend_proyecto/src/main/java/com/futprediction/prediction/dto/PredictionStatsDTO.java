package com.futprediction.prediction.dto;

public record PredictionStatsDTO(
        int totalPredictions, int correct, int failed, int pending, double accuracyRate) {}
