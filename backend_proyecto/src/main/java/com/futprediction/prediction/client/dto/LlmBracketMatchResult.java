package com.futprediction.prediction.client.dto;

public record LlmBracketMatchResult(
        String homeTeam, String awayTeam, String winner, Integer homeScore, Integer awayScore) {}
