package com.futprediction.match.dto;

import java.time.LocalDateTime;

public record MatchResponseDTO(
        Long id,
        Long idEquipoLocal,
        Long idEquipoVisitante,
        LocalDateTime fechaHora,
        String estadio,
        String fase,
        String estado
) {
}
