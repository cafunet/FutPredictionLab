package com.futprediction.prediction.dto;

import java.util.List;

public record BracketSimulationDTO(
        List<BracketMatchDTO> quarterFinalsLeft,
        List<BracketMatchDTO> quarterFinalsRight,
        BracketMatchDTO semiFinalLeft,
        BracketMatchDTO semiFinalRight,
        BracketMatchDTO finalMatch,
        BracketTeamDTO champion,
        String reasoning) {}
