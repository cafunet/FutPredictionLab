package com.futprediction.match.dto;

public record MatchViewDTO(
        String id,
        String local,
        String visitor,
        String localFlag,
        String visitorFlag,
        String localBanderaUrl,
        String visitorBanderaUrl,
        int localScore,
        int visitorScore,
        String status,
        String date,
        String estadio,
        String fase,
        int liveMinute,
        int stoppageTime,
        boolean halftimeBreak,
        java.util.List<MatchEventViewDTO> events,
        String scheduledBy
) {
}
