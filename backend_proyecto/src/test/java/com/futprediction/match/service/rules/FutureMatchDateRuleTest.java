package com.futprediction.match.service.rules;

import com.futprediction.match.dto.MatchCreateRequestDTO;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FutureMatchDateRuleTest {
    private final FutureMatchDateRule rule = new FutureMatchDateRule();
    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

    @Test
    void rechazaUnPartidoConFechaPasadaEnColombia() {
        var request = partido(LocalDateTime.now(COLOMBIA).minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> rule.validate(request));
    }

    @Test
    void permiteUnPartidoConFechaFuturaEnColombia() {
        var request = partido(LocalDateTime.now(COLOMBIA).plusDays(1));
        assertDoesNotThrow(() -> rule.validate(request));
    }

    private MatchCreateRequestDTO partido(LocalDateTime fecha) {
        return new MatchCreateRequestDTO(1L, 2L, fecha, "Estadio de prueba", "Grupos");
    }
}
