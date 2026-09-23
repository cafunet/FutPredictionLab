package com.futprediction.prediction.service;

import com.futprediction.prediction.client.PredictionLlmClient;
import com.futprediction.prediction.client.dto.LlmBracketMatchResult;
import com.futprediction.prediction.client.dto.LlmBracketSimulationResponse;
import com.futprediction.prediction.client.dto.LlmBracketTeamInput;
import com.futprediction.prediction.config.PredictionLlmProperties;
import com.futprediction.prediction.dto.BracketMatchDTO;
import com.futprediction.prediction.dto.BracketSimulationDTO;
import com.futprediction.prediction.dto.BracketTeamDTO;
import com.futprediction.shared.util.FlagUrlResolver;
import com.futprediction.teams.entity.Equipo;
import com.futprediction.teams.repository.EquipoRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BracketSimulationService {

    private final PredictionLlmClient llmClient;
    private final PredictionLlmProperties llmProperties;
    private final EquipoRepository equipoRepository;

    public BracketSimulationService(
            PredictionLlmClient llmClient,
            PredictionLlmProperties llmProperties,
            EquipoRepository equipoRepository) {
        this.llmClient = llmClient;
        this.llmProperties = llmProperties;
        this.equipoRepository = equipoRepository;
    }

    public BracketSimulationDTO simulate() {
        if (!llmProperties.enabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "El servicio de predicciones IA no está disponible");
        }

        List<Equipo> top8 = equipoRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Equipo::getRankingFifa))
                .limit(8)
                .toList();

        if (top8.size() < 8) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Se necesitan al menos 8 equipos en la base de datos para simular la fase final");
        }

        Map<String, Equipo> byName = new HashMap<>();
        for (Equipo e : equipoRepository.findAll()) {
            byName.put(normalize(e.getNombre()), e);
            byName.putIfAbsent(normalize(e.getPais()), e);
        }

        List<LlmBracketTeamInput> llmTeams = top8.stream()
                .map(e -> new LlmBracketTeamInput(e.getNombre(), e.getPais(), e.getRankingFifa()))
                .toList();

        LlmBracketSimulationResponse llm = llmClient.simulateBracket(llmTeams);

        if (llm.quarterFinals() == null || llm.quarterFinals().size() < 4) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "La IA no devolvió cuartos de final completos");
        }

        List<BracketMatchDTO> qfLeft = List.of(
                toMatchDto(llm.quarterFinals().get(0), byName, top8),
                toMatchDto(llm.quarterFinals().get(1), byName, top8));
        List<BracketMatchDTO> qfRight = List.of(
                toMatchDto(llm.quarterFinals().get(2), byName, top8),
                toMatchDto(llm.quarterFinals().get(3), byName, top8));

        BracketMatchDTO sfLeft = llm.semiFinals() != null && llm.semiFinals().size() >= 1
                ? toMatchDto(llm.semiFinals().get(0), byName, top8)
                : null;
        BracketMatchDTO sfRight = llm.semiFinals() != null && llm.semiFinals().size() >= 2
                ? toMatchDto(llm.semiFinals().get(1), byName, top8)
                : null;
        BracketMatchDTO fin = llm.finalMatch() != null ? toMatchDto(llm.finalMatch(), byName, top8) : null;

        String championName = llm.champion() != null ? llm.champion() : (fin != null ? fin.winner().name() : null);
        BracketTeamDTO champion = championName != null ? toTeamDto(championName, byName, top8) : null;

        return new BracketSimulationDTO(
                qfLeft,
                qfRight,
                sfLeft,
                sfRight,
                fin,
                champion,
                llm.reasoning() != null ? llm.reasoning() : "");
    }

    private BracketMatchDTO toMatchDto(LlmBracketMatchResult m, Map<String, Equipo> byName, List<Equipo> top8) {
        return new BracketMatchDTO(
                toTeamDto(m.homeTeam(), byName, top8),
                toTeamDto(m.awayTeam(), byName, top8),
                toTeamDto(m.winner(), byName, top8),
                m.homeScore(),
                m.awayScore());
    }

    private BracketTeamDTO toTeamDto(String name, Map<String, Equipo> byName, List<Equipo> top8) {
        Equipo e = resolveEquipo(name, byName, top8);
        if (e != null) {
            return new BracketTeamDTO(
                    e.getNombre(),
                    e.getPais(),
                    FlagUrlResolver.resolve(e.getBanderaUrl(), e.getPais(), e.getNombre()),
                    e.getRankingFifa());
        }
        return new BracketTeamDTO(
                name,
                name,
                FlagUrlResolver.resolve(null, name, name),
                99);
    }

    private Equipo resolveEquipo(String name, Map<String, Equipo> byName, List<Equipo> top8) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Equipo direct = byName.get(normalize(name));
        if (direct != null) {
            return direct;
        }
        String key = normalize(name);
        for (Equipo e : top8) {
            if (normalize(e.getNombre()).equals(key) || normalize(e.getPais()).equals(key)) {
                return e;
            }
        }
        for (Equipo e : byName.values()) {
            if (normalize(e.getNombre()).contains(key) || key.contains(normalize(e.getNombre()))) {
                return e;
            }
        }
        return null;
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }
}
