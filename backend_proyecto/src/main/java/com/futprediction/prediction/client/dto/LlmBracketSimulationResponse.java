package com.futprediction.prediction.client.dto;

import java.util.List;

public record LlmBracketSimulationResponse(
        List<LlmBracketMatchResult> quarterFinals,
        List<LlmBracketMatchResult> semiFinals,
        LlmBracketMatchResult finalMatch,
        String champion,
        String reasoning) {}
