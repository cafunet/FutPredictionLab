package com.futprediction.match.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record MatchCreateRequestDTO(
        @NotNull Long idEquipoLocal,
        @NotNull Long idEquipoVisitante,
        @NotNull LocalDateTime fechaHora,
        @NotBlank @Size(max = 120) String estadio,
        @NotBlank @Size(max = 50) String fase
) {
}
