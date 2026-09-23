package com.futprediction.prediction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SavePredictionRequestDTO(
        @NotBlank String matchId,
        @NotBlank String predictedResult,
        @NotNull Double accuracy,
        String justification,
        Double localWinProbability,
        Double drawProbability,
        Double visitorWinProbability,
        Integer predictedLocalScore,
        Integer predictedVisitorScore,
        String modelVersion) {}
