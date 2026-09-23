package com.futprediction.prediction.dto;

import java.util.List;

public record PredictionResultDTO(
        String matchId,
        double localWinProbability,
        double drawProbability,
        double visitorWinProbability,
        String justification,
        double confidence,
        String modelVersion,
        String predictedWinner,
        Integer predictedLocalScore,
        Integer predictedVisitorScore,
        List<PredictionFactorDTO> factors,
        H2HComparisonDTO h2hComparison) {

    public record PredictionFactorDTO(String type, String description) {}

    public record H2HComparisonDTO(
            int localRank,
            int visitorRank,
            double localEfficiency,
            double visitorEfficiency,
            double localGoalsPerMatch,
            double visitorGoalsPerMatch) {}
}
