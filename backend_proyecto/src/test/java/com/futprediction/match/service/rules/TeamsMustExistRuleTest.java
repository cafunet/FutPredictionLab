package com.futprediction.match.service.rules;

import com.futprediction.match.dto.MatchCreateRequestDTO;
import com.futprediction.teams.repository.EquipoRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamsMustExistRuleTest {
    @Mock private EquipoRepository repository;
    @InjectMocks private TeamsMustExistRule rule;

    private final MatchCreateRequestDTO request = new MatchCreateRequestDTO(
            1L, 2L, LocalDateTime.of(2030, 6, 1, 15, 0), "Estadio de prueba", "Grupos");

    @Test
    void permiteProgramarCuandoAmbosEquiposExisten() {
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.existsById(2L)).thenReturn(true);
        assertDoesNotThrow(() -> rule.validate(request));
    }

    @Test
    void rechazaCuandoNoExisteElEquipoLocal() {
        when(repository.existsById(1L)).thenReturn(false);
        assertThrows(EntityNotFoundException.class, () -> rule.validate(request));
    }

    @Test
    void rechazaCuandoNoExisteElEquipoVisitante() {
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.existsById(2L)).thenReturn(false);
        assertThrows(EntityNotFoundException.class, () -> rule.validate(request));
    }
}
