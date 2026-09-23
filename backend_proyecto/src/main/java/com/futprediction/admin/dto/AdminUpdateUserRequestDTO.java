package com.futprediction.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpdateUserRequestDTO(
        @NotBlank @Size(max = 120) @JsonProperty("name") String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(min = 6, max = 50) String password,
        @NotBlank @Pattern(regexp = "ADMIN|USER", message = "debe ser ADMIN o USER") String role
) {
}
