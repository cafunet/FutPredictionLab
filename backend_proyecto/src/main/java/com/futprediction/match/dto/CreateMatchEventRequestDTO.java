package com.futprediction.match.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateMatchEventRequestDTO(
        @NotBlank
                @Pattern(regexp = "^(?i)(goal|yellow|red)$", message = "Tipo debe ser goal, yellow o red")
                String type,
        @NotNull @Min(0) @Max(120) Integer minute,
        @NotBlank String playerName,
        @NotBlank
                @Pattern(regexp = "^(?i)(local|visitor)$", message = "Equipo debe ser local o visitor")
                String teamSide) {}
