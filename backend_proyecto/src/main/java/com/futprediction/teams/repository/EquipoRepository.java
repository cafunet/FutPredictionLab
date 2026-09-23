package com.futprediction.teams.repository;

import com.futprediction.teams.entity.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
}
