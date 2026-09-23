package com.futprediction.match.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateLiveScoreRequestDTO(
        @NotNull @Min(0) @Max(50) Integer localScore,
        @NotNull @Min(0) @Max(50) Integer visitorScore) {}
