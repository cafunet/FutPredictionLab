package com.futprediction.admin.service;

import com.futprediction.admin.dto.AdminCreateUserRequestDTO;
import com.futprediction.admin.dto.AdminCreateUserResponseDTO;
import com.futprediction.admin.dto.UpdateUserRoleRequestDTO;
import com.futprediction.auth.dto.UserResponseDTO;
import com.futprediction.auth.entity.Usuario;
import com.futprediction.auth.repository.UsuarioRepository;
import com.futprediction.auth.service.AuthService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminUserService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            AuthService authService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    public List<UserResponseDTO> listUsers() {
        return usuarioRepository.findAll().stream()
                .sorted(Comparator.comparing(Usuario::getId))
                .map(authService::toUserResponse)
                .toList();
    }

    public UserResponseDTO updateRole(Long userId, UpdateUserRoleRequestDTO request) {
        Usuario usuario = usuarioRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Usuario.Rol newRol;
        try {
            newRol = Usuario.Rol.valueOf(request.role());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol invalido");
        }

        long adminCount = usuarioRepository.countByRol(Usuario.Rol.ADMIN);
        if (usuario.getRol() == Usuario.Rol.ADMIN
                && newRol == Usuario.Rol.USER
                && adminCount <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "No se puede quitar el rol de administrador al unico administrador del sistema");
        }

        usuario.setRol(newRol);
        Usuario saved = usuarioRepository.save(usuario);
        return authService.toUserResponse(saved);
    }

    public void deleteUser(Long userId, Long actorId) {
        if (userId.equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes eliminar tu propia cuenta");
        }

        Usuario usuario = usuarioRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (usuario.getRol() == Usuario.Rol.ADMIN && usuarioRepository.countByRol(Usuario.Rol.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar el unico administrador del sistema");
        }

        usuarioRepository.delete(usuario);
    }

    public UserResponseDTO createUser(AdminCreateUserRequestDTO request) {
        if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya esta registrado");
        }

        Usuario.Rol rol;
        try {
            rol = Usuario.Rol.valueOf(request.role());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol invalido");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.fullName());
        usuario.setEmail(request.email().toLowerCase());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(rol);
        usuario.setFechaRegistro(LocalDateTime.now());

        Usuario saved = usuarioRepository.save(usuario);
        return authService.toUserResponse(saved);
    }

    public UserResponseDTO updateUser(Long userId, com.futprediction.admin.dto.AdminUpdateUserRequestDTO request, Usuario actor) {
        Usuario usuario = usuarioRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!usuario.getEmail().equalsIgnoreCase(request.email()) && usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya esta registrado por otro usuario");
        }

        Usuario.Rol newRol;
        try {
            newRol = Usuario.Rol.valueOf(request.role());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol invalido");
        }

        long adminCount = usuarioRepository.countByRol(Usuario.Rol.ADMIN);
        if (usuario.getRol() == Usuario.Rol.ADMIN && newRol == Usuario.Rol.USER && adminCount <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede quitar el rol de administrador al unico administrador del sistema");
        }

        usuario.setNombre(request.fullName());
        usuario.setEmail(request.email().toLowerCase());
        
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        usuario.setRol(newRol);
        usuario.setModificadoPor(actor.getEmail());
        usuario.setFechaModificacion(LocalDateTime.now());

        Usuario saved = usuarioRepository.save(usuario);
        return authService.toUserResponse(saved);
    }
}
