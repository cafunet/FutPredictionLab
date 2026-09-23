package com.futprediction.prediction.dto;

public record PredictionHistoryDTO(
        String id,
        String match,
        String localTeam,
        String visitorTeam,
        String predicted,
        String result,
        String status,
        String date,
        double accuracy,
        String justification,
        Double localWinProbability,
        Double drawProbability,
        Double visitorWinProbability,
        Integer predictedLocalScore,
        Integer predictedVisitorScore,
        String modelVersion) {}
