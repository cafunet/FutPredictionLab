package com.futprediction.match.repository;

import com.futprediction.match.entity.Partido;
import com.futprediction.match.entity.Partido.EstadoPartido;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartidoRepository extends JpaRepository<Partido, Long> {

    @Query(
            "SELECT DISTINCT p FROM Partido p JOIN FETCH p.equipoLocal JOIN FETCH p.equipoVisitante ORDER BY p.fecha ASC")
    List<Partido> findAllWithEquipos();

    @Query(
            "SELECT p FROM Partido p JOIN FETCH p.equipoLocal JOIN FETCH p.equipoVisitante WHERE p.id = :id")
    Optional<Partido> findWithEquiposById(@Param("id") Long id);

    @Query(
            """
            SELECT p FROM Partido p
            JOIN FETCH p.equipoLocal
            JOIN FETCH p.equipoVisitante
            WHERE p.estado = :estado AND p.fecha <= :ahora
            """)
    List<Partido> findProgramadosListosParaIniciar(
            @Param("estado") EstadoPartido estado, @Param("ahora") LocalDateTime ahora);
}
