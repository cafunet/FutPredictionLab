package com.futprediction.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRoleRequestDTO(
        @NotBlank @Pattern(regexp = "ADMIN|USER", message = "debe ser ADMIN o USER") String role
) {
}
