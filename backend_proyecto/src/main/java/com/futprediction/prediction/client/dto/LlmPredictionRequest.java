package com.futprediction.prediction.client.dto;

public record LlmPredictionRequest(
        String homeTeamName,
        String awayTeamName,
        String homeCountry,
        String awayCountry,
        Integer homeFifaRank,
        Integer awayFifaRank,
        String matchPhase,
        String stadium,
        String homeGroup,
        String awayGroup) {}
