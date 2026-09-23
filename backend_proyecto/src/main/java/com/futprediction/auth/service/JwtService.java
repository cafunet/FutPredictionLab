package com.futprediction.auth.service;

import com.futprediction.auth.entity.Usuario;
import com.futprediction.shared.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtUtils jwtUtils;
    private final long expirationMs;

    public JwtService(JwtUtils jwtUtils, @Value("${jwt.expiration-ms}") long expirationMs) {
        this.jwtUtils = jwtUtils;
        this.expirationMs = expirationMs;
    }

    public String generateToken(Usuario usuario) {
        return jwtUtils.generateToken(usuario);
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
