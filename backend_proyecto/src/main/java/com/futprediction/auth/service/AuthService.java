package com.futprediction.auth.service;

import com.futprediction.auth.dto.AuthResponseDTO;
import com.futprediction.auth.dto.LoginRequestDTO;
import com.futprediction.auth.dto.RegisterRequestDTO;
import com.futprediction.auth.dto.UserResponseDTO;
import com.futprediction.auth.entity.Usuario;
import com.futprediction.auth.repository.UsuarioRepository;
import com.futprediction.match.support.MatchColombiaTime;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        Authentication authResult =
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        Usuario usuario = (Usuario) authResult.getPrincipal();

        LOGGER.info("Inicio de sesion exitoso para email={}", usuario.getEmail());

        String token = jwtService.generateToken(usuario);
        return new AuthResponseDTO(token, toUserResponse(usuario));
    }

    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya esta registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email().toLowerCase());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(Usuario.Rol.USER);
        usuario.setFechaRegistro(LocalDateTime.now());

        Usuario saved = usuarioRepository.save(usuario);
        String token = jwtService.generateToken(saved);

        return new AuthResponseDTO(token, toUserResponse(saved));
    }

    public AuthResponseDTO googleLogin(com.futprediction.auth.dto.GoogleAuthRequestDTO request) {
        try {
            com.google.api.client.http.HttpTransport transport = new com.google.api.client.http.javanet.NetHttpTransport();
            com.google.api.client.json.JsonFactory jsonFactory = com.google.api.client.json.gson.GsonFactory.getDefaultInstance();

            com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier verifier = new com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .build();

            com.google.api.client.googleapis.auth.oauth2.GoogleIdToken idToken = verifier.verify(request.idToken());
            if (idToken != null) {
                com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");

                Usuario usuario = usuarioRepository.findByEmail(email.toLowerCase()).orElse(null);
                if (usuario == null) {
                    usuario = new Usuario();
                    usuario.setNombre(name != null ? name : "Usuario Google");
                    usuario.setEmail(email.toLowerCase());
                    usuario.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
                    usuario.setRol(Usuario.Rol.USER);
                    usuario.setFechaRegistro(LocalDateTime.now());
                    usuario = usuarioRepository.save(usuario);
                }

                String token = jwtService.generateToken(usuario);
                return new AuthResponseDTO(token, toUserResponse(usuario));
            } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de Google inválido");
            }
        } catch (Exception e) {
            LOGGER.error("Error al verificar token de Google", e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error al verificar token de Google");
        }
    }

    public UserResponseDTO toUserResponse(Usuario usuario) {
        String createdAt = MatchColombiaTime.toIsoOffset(usuario.getFechaRegistro());
        String modifiedAt = MatchColombiaTime.toIsoOffset(usuario.getFechaModificacion());

        return new UserResponseDTO(
                String.valueOf(usuario.getId()),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol().name(),
                createdAt,
                usuario.getModificadoPor(),
                modifiedAt);
    }
}
