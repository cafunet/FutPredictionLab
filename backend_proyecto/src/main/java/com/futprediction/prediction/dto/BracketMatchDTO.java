package com.futprediction.prediction.dto;

public record BracketMatchDTO(
        BracketTeamDTO home,
        BracketTeamDTO away,
        BracketTeamDTO winner,
        Integer homeScore,
        Integer awayScore) {}
