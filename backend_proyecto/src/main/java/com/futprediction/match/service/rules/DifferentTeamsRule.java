package com.futprediction.match.service.rules;

import com.futprediction.match.dto.MatchCreateRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class DifferentTeamsRule implements MatchSchedulingRule {

    @Override
    public void validate(MatchCreateRequestDTO request) {
        if (request.idEquipoLocal().equals(request.idEquipoVisitante())) {
            throw new IllegalArgumentException("El equipo local y visitante no pueden ser el mismo");
        }
    }
}
