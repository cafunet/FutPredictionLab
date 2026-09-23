package com.futprediction.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserResponseDTO(
        String id,
        @JsonProperty("fullName") String fullName,
        String email,
        String role,
        String createdAt,
        String modifiedBy,
        String modifiedAt
) {
}
