package com.futprediction.match.service.rules;

import com.futprediction.match.dto.MatchCreateRequestDTO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DifferentTeamsRuleTest {
    private final DifferentTeamsRule rule = new DifferentTeamsRule();

    @Test
    void rechazaUnEquipoJugandoContraSiMismo() {
        var request = partido(1L, 1L);
        assertThrows(IllegalArgumentException.class, () -> rule.validate(request));
    }

    @Test
    void permiteEquiposDiferentes() {
        assertDoesNotThrow(() -> rule.validate(partido(1L, 2L)));
    }

    private MatchCreateRequestDTO partido(Long local, Long visitante) {
        return new MatchCreateRequestDTO(local, visitante,
                LocalDateTime.of(2030, 6, 1, 15, 0), "Estadio de prueba", "Grupos");
    }
}
