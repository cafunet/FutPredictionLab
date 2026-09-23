package com.futprediction.auth.controller;

import com.futprediction.auth.dto.AuthResponseDTO;
import com.futprediction.auth.dto.LoginRequestDTO;
import com.futprediction.auth.dto.RegisterRequestDTO;
import com.futprediction.auth.dto.UserResponseDTO;
import com.futprediction.auth.entity.Usuario;
import com.futprediction.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(authService.toUserResponse(usuario));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponseDTO> googleLogin(@Valid @RequestBody com.futprediction.auth.dto.GoogleAuthRequestDTO request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }
}
