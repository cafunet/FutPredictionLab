package com.futprediction.match.repository;

import com.futprediction.match.entity.EventoPartido;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoPartidoRepository extends JpaRepository<EventoPartido, Long> {

    List<EventoPartido> findByPartido_IdOrderByMinutoDescIdDesc(Long partidoId);

    List<EventoPartido> findByPartido_IdIn(Collection<Long> partidoIds);

    void deleteByPartido_Id(Long partidoId);
}
