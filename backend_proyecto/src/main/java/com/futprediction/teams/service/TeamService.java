package com.futprediction.teams.service;

import com.futprediction.teams.dto.TeamCreateRequestDTO;
import com.futprediction.teams.dto.TeamResponseDTO;
import com.futprediction.teams.entity.Equipo;
import com.futprediction.teams.repository.EquipoRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeamService {

    private final EquipoRepository equipoRepository;

    public TeamService(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
    }

    public TeamResponseDTO createTeam(TeamCreateRequestDTO request) {
        if (equipoRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un equipo con ese nombre");
        }

        Equipo equipo = new Equipo();
        equipo.setNombre(request.nombre().trim());
        equipo.setPais(request.pais().trim());
        equipo.setGrupo(request.grupo().trim().toUpperCase());
        equipo.setRankingFifa(request.rankingFifa());
        equipo.setBanderaUrl(request.banderaUrl());

        Equipo saved = equipoRepository.save(equipo);
        return toResponse(saved);
    }

    public List<TeamResponseDTO> getAllTeams() {
        return equipoRepository.findAll().stream().map(this::toResponse).toList();
    }

    public TeamResponseDTO getTeamById(Long id) {
        Equipo equipo = equipoRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipo no encontrado"));
        return toResponse(equipo);
    }

    public TeamResponseDTO updateTeam(Long id, TeamCreateRequestDTO request) {
        Equipo equipo = equipoRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipo no encontrado"));

        if (!equipo.getNombre().equalsIgnoreCase(request.nombre()) && equipoRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otro equipo con ese nombre");
        }

        equipo.setNombre(request.nombre().trim());
        equipo.setPais(request.pais().trim());
        equipo.setGrupo(request.grupo().trim().toUpperCase());
        equipo.setRankingFifa(request.rankingFifa());
        equipo.setBanderaUrl(request.banderaUrl());

        Equipo saved = equipoRepository.save(equipo);
        return toResponse(saved);
    }

    public void deleteTeam(Long id) {
        Equipo equipo = equipoRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipo no encontrado"));
        equipoRepository.delete(equipo);
    }

    private TeamResponseDTO toResponse(Equipo equipo) {
        return new TeamResponseDTO(
                equipo.getId(),
                equipo.getNombre(),
                equipo.getPais(),
                equipo.getGrupo(),
                equipo.getRankingFifa(),
                equipo.getBanderaUrl());
    }
}
