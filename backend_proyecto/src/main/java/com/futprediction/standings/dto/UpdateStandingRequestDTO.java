package com.futprediction.standings.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateStandingRequestDTO(
        @NotNull @Min(0) Integer pts,
        @NotNull @Min(0) Integer pj,
        @NotNull @Min(0) Integer pg,
        @NotNull @Min(0) Integer pe,
        @NotNull @Min(0) Integer pp,
        @NotNull @Min(0) Integer gf,
        @NotNull @Min(0) Integer gc) {}
