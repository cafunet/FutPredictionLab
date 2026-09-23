package com.futprediction.auth.repository;

import com.futprediction.auth.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRol(Usuario.Rol rol);
}
