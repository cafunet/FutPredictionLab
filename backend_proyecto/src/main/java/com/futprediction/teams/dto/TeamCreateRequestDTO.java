package com.futprediction.teams.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TeamCreateRequestDTO(
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Size(max = 120) String pais,
        @NotBlank @Size(max = 10) String grupo,
        @NotNull @Min(0) @Max(999) Integer rankingFifa,
        @Size(max = 255) String banderaUrl
) {
}
