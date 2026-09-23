package com.futprediction.teams.service;

import com.futprediction.teams.dto.TeamCreateRequestDTO;
import com.futprediction.teams.entity.Equipo;
import com.futprediction.teams.repository.EquipoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {
    @Mock private EquipoRepository repository;
    @InjectMocks private TeamService service;

    @Test
    void creaEquipoConNombreLimpioYGrupoEnMayusculas() {
        var request = new TeamCreateRequestDTO(" Colombia ", " Colombia ", " a ", 10, null);
        when(repository.save(any(Equipo.class))).thenAnswer(invocation -> {
            Equipo equipo = invocation.getArgument(0);
            // Simula solamente la asignacion de identidad que haria PostgreSQL.
            ReflectionTestUtils.setField(equipo, "id", 42L);
            return equipo;
        });

        var response = service.createTeam(request);

        assertAll(
                () -> assertEquals(Long.valueOf(42), response.id()),
                () -> assertEquals("Colombia", response.nombre()),
                () -> assertEquals("Colombia", response.pais()),
                () -> assertEquals("A", response.grupo()),
                () -> assertEquals(Integer.valueOf(10), response.rankingFifa()));
        verify(repository).save(any(Equipo.class));
    }

    @Test
    void rechazaCrearUnNombreDuplicadoSinGuardar() {
        when(repository.existsByNombreIgnoreCase("Colombia")).thenReturn(true);

        var error = assertThrows(ResponseStatusException.class,
                () -> service.createTeam(datos("Colombia")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).save(any(Equipo.class));
    }

    @Test
    void devuelveNoEncontradoCuandoElEquipoNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        var error = assertThrows(ResponseStatusException.class,
                () -> service.getTeamById(99L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void rechazaRenombrarConNombreDeOtroEquipoSinModificarElOriginal() {
        Equipo original = equipo("Colombia");
        when(repository.findById(1L)).thenReturn(Optional.of(original));
        when(repository.existsByNombreIgnoreCase("Brasil")).thenReturn(true);

        var error = assertThrows(ResponseStatusException.class,
                () -> service.updateTeam(1L, datos("Brasil")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("Colombia", original.getNombre());
        verify(repository, never()).save(any(Equipo.class));
    }

    @Test
    void permiteActualizarElMismoNombreConDiferenteUsoDeMayusculas() {
        Equipo original = equipo("Colombia");
        when(repository.findById(1L)).thenReturn(Optional.of(original));
        // Incluso si existe ese nombre, corresponde al propio equipo.
        when(repository.save(any(Equipo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateTeam(1L,
                new TeamCreateRequestDTO("COLOMBIA", "Colombia", " b ", 15, null));

        assertAll(
                () -> assertEquals("COLOMBIA", response.nombre()),
                () -> assertEquals("B", response.grupo()),
                () -> assertEquals(Integer.valueOf(15), response.rankingFifa()));
        verify(repository).save(original);
    }

    @Test
    void rechazaEliminarUnEquipoInexistenteSinEjecutarBorrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        var error = assertThrows(ResponseStatusException.class,
                () -> service.deleteTeam(99L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).delete(any(Equipo.class));
    }

    private TeamCreateRequestDTO datos(String nombre) {
        return new TeamCreateRequestDTO(nombre, nombre, "A", 10, null);
    }

    private Equipo equipo(String nombre) {
        Equipo equipo = new Equipo();
        equipo.setNombre(nombre);
        equipo.setPais(nombre);
        equipo.setGrupo("A");
        equipo.setRankingFifa(10);
        return equipo;
    }
}
