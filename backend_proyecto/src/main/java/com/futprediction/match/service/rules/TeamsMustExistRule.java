package com.futprediction.match.service.rules;

import com.futprediction.match.dto.MatchCreateRequestDTO;
import com.futprediction.teams.repository.EquipoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class TeamsMustExistRule implements MatchSchedulingRule {

    private final EquipoRepository equipoRepository;

    public TeamsMustExistRule(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
    }

    @Override
    public void validate(MatchCreateRequestDTO request) {
        if (!equipoRepository.existsById(request.idEquipoLocal())) {
            throw new EntityNotFoundException("Equipo local no encontrado");
        }
        if (!equipoRepository.existsById(request.idEquipoVisitante())) {
            throw new EntityNotFoundException("Equipo visitante no encontrado");
        }
    }
}
