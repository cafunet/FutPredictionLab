package com.futprediction.admin.dto;

import com.futprediction.auth.dto.UserResponseDTO;

/**
 * Incluye la contraseña temporal solo en la respuesta de creación (una vez).
 */
public record AdminCreateUserResponseDTO(UserResponseDTO user, String temporaryPassword) {
}
