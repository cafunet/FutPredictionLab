package com.futprediction.teams.dto;

public record TeamResponseDTO(
        Long id,
        String nombre,
        String pais,
        String grupo,
        Integer rankingFifa,
        String banderaUrl
) {
}
