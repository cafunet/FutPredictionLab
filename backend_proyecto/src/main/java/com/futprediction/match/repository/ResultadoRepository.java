package com.futprediction.match.repository;

import com.futprediction.match.entity.Resultado;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultadoRepository extends JpaRepository<Resultado, Long> {

    Optional<Resultado> findByPartido_Id(Long idPartido);

    List<Resultado> findByPartido_IdIn(Collection<Long> partidoIds);
}
