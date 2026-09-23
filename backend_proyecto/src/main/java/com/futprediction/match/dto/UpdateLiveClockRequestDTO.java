package com.futprediction.match.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateLiveClockRequestDTO(
        @NotNull @Min(0) @Max(120) Integer minute,
        @NotNull @Min(0) @Max(30) Integer stoppageTime) {}
