package com.futprediction.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequestDTO(
        @NotBlank(message = "El idToken no puede estar vacío")
        String idToken
) {
}
