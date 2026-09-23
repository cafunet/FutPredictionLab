package com.futprediction.prediction.repository;

import com.futprediction.prediction.entity.Prediccion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrediccionRepository extends JpaRepository<Prediccion, Long> {

    @Query(
            """
            SELECT p FROM Prediccion p
            JOIN FETCH p.partido partido
            JOIN FETCH partido.equipoLocal
            JOIN FETCH partido.equipoVisitante
            WHERE p.usuario.id = :userId
            ORDER BY p.fechaPrediccion DESC
            """)
    List<Prediccion> findByUsuarioIdWithPartido(@Param("userId") Long userId);

    @Query(
            """
            SELECT p FROM Prediccion p
            JOIN FETCH p.partido partido
            JOIN FETCH partido.equipoLocal
            JOIN FETCH partido.equipoVisitante
            WHERE p.id = :id AND p.usuario.id = :userId
            """)
    Optional<Prediccion> findByIdAndUsuarioIdWithPartido(@Param("id") Long id, @Param("userId") Long userId);

    Optional<Prediccion> findByUsuario_IdAndPartido_Id(Long usuarioId, Long partidoId);

    @Query(
            """
            SELECT p FROM Prediccion p
            JOIN FETCH p.usuario u
            JOIN FETCH p.partido partido
            JOIN FETCH partido.equipoLocal
            JOIN FETCH partido.equipoVisitante
            ORDER BY p.fechaPrediccion DESC
            """)
    List<Prediccion> findAllWithDetails();
}
