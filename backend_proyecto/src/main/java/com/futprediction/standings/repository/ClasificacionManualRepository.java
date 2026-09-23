package com.futprediction.standings.repository;

import com.futprediction.standings.entity.ClasificacionManual;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClasificacionManualRepository extends JpaRepository<ClasificacionManual, Long> {

    List<ClasificacionManual> findByActivoTrue();
}
