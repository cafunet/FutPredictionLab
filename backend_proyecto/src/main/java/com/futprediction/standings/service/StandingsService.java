package com.futprediction.standings.service;

import com.futprediction.auth.entity.Usuario;
import com.futprediction.match.entity.Partido;
import com.futprediction.match.entity.Partido.EstadoPartido;
import com.futprediction.match.entity.Resultado;
import com.futprediction.match.repository.PartidoRepository;
import com.futprediction.match.repository.ResultadoRepository;
import com.futprediction.match.support.MatchColombiaTime;
import com.futprediction.standings.dto.StandingDTO;
import com.futprediction.standings.dto.UpdateStandingRequestDTO;
import com.futprediction.standings.entity.ClasificacionManual;
import com.futprediction.standings.repository.ClasificacionManualRepository;
import com.futprediction.teams.entity.Equipo;
import com.futprediction.teams.repository.EquipoRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StandingsService {

    private final EquipoRepository equipoRepository;
    private final PartidoRepository partidoRepository;
    private final ResultadoRepository resultadoRepository;
    private final ClasificacionManualRepository clasificacionManualRepository;

    public StandingsService(
            EquipoRepository equipoRepository,
            PartidoRepository partidoRepository,
            ResultadoRepository resultadoRepository,
            ClasificacionManualRepository clasificacionManualRepository) {
        this.equipoRepository = equipoRepository;
        this.partidoRepository = partidoRepository;
        this.resultadoRepository = resultadoRepository;
        this.clasificacionManualRepository = clasificacionManualRepository;
    }

    public Map<String, List<StandingDTO>> getAllStandings() {
        StandingsContext ctx = buildContext();
        Map<String, List<StandingDTO>> result = new LinkedHashMap<>();
        ctx.teamsByGroup().keySet().stream().sorted().forEach(g -> result.put(g, buildGroupStandings(g, ctx)));
        return result;
    }

    public List<StandingDTO> getStandingsForGroup(String group) {
        return buildGroupStandings(group.trim().toUpperCase(), buildContext());
    }

    private StandingsContext buildContext() {
        List<Equipo> allTeams = equipoRepository.findAll();
        List<Partido> allMatches = partidoRepository.findAllWithEquipos();

        List<Long> relevantMatchIds = allMatches.stream()
                .filter(p -> p.getEstado() == EstadoPartido.EN_CURSO
                        || p.getEstado() == EstadoPartido.SUSPENDIDO
                        || p.getEstado() == EstadoPartido.FINALIZADO)
                .map(Partido::getId)
                .toList();

        Map<Long, Resultado> resultadosByMatchId = relevantMatchIds.isEmpty()
                ? Map.of()
                : resultadoRepository.findByPartido_IdIn(relevantMatchIds).stream()
                        .collect(Collectors.toMap(r -> r.getPartido().getId(), r -> r));

        Map<Long, ClasificacionManual> manualByTeam = clasificacionManualRepository.findAll().stream()
                .collect(Collectors.toMap(ClasificacionManual::getIdEquipo, m -> m));

        Map<String, List<Equipo>> teamsByGroup = allTeams.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getGrupo().trim().toUpperCase(),
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            list.sort(Comparator.comparing(Equipo::getNombre));
                            return list;
                        })));

        return new StandingsContext(allMatches, resultadosByMatchId, manualByTeam, teamsByGroup);
    }

    private List<StandingDTO> buildGroupStandings(String normalizedGroup, StandingsContext ctx) {
        List<Equipo> teams = ctx.teamsByGroup().getOrDefault(normalizedGroup, List.of());
        if (teams.isEmpty()) {
            return List.of();
        }

        Map<Long, MutableStats> statsByTeam = new HashMap<>();
        for (Equipo team : teams) {
            statsByTeam.put(team.getId(), MutableStats.zero());
        }

        Set<Long> teamsInLiveMatch = new HashSet<>();
        boolean groupHasLive = false;

        List<Partido> groupMatches = ctx.allMatches().stream()
                .filter(p -> isSameGroupMatch(p, normalizedGroup))
                .filter(p -> p.getEstado() == EstadoPartido.EN_CURSO
                        || p.getEstado() == EstadoPartido.SUSPENDIDO
                        || p.getEstado() == EstadoPartido.FINALIZADO)
                .toList();

        for (Partido partido : groupMatches) {
            Resultado resultado = ctx.resultadosByMatchId().get(partido.getId());
            if (resultado == null) {
                continue;
            }

            if (partido.getEstado() == EstadoPartido.EN_CURSO) {
                groupHasLive = true;
                teamsInLiveMatch.add(partido.getEquipoLocal().getId());
                teamsInLiveMatch.add(partido.getEquipoVisitante().getId());
            }

            applyMatch(
                    statsByTeam,
                    partido.getEquipoLocal().getId(),
                    partido.getEquipoVisitante().getId(),
                    resultado);
        }

        List<StandingDTO> standings = new ArrayList<>();
        for (Equipo team : teams) {
            ClasificacionManual manual = ctx.manualByTeam().get(team.getId());
            MutableStats stats = statsByTeam.get(team.getId());
            boolean manualOverride = manual != null && manual.isActivo();
            if (manualOverride) {
                stats = MutableStats.fromManual(manual);
            }

            boolean teamProvisional =
                    groupHasLive && teamsInLiveMatch.contains(team.getId()) && !manualOverride;
            standings.add(toDto(team, stats, teamProvisional, manualOverride, manual));
        }

        return sortWithTiebreakers(standings, groupMatches, ctx.resultadosByMatchId());
    }

    private record StandingsContext(
            List<Partido> allMatches,
            Map<Long, Resultado> resultadosByMatchId,
            Map<Long, ClasificacionManual> manualByTeam,
            Map<String, List<Equipo>> teamsByGroup) {}

    @Transactional
    public StandingDTO updateManualStanding(Long teamId, UpdateStandingRequestDTO request, Usuario actor) {
        Equipo team = equipoRepository
                .findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Equipo no encontrado"));

        ClasificacionManual manual = clasificacionManualRepository
                .findById(teamId)
                .orElseGet(() -> {
                    ClasificacionManual created = new ClasificacionManual();
                    created.setIdEquipo(teamId);
                    return created;
                });

        manual.setActivo(true);
        manual.setPts(request.pts());
        manual.setPj(request.pj());
        manual.setPg(request.pg());
        manual.setPe(request.pe());
        manual.setPp(request.pp());
        manual.setGf(request.gf());
        manual.setGc(request.gc());
        manual.setIdUsuarioUltimo(actor != null ? actor.getId() : null);
        manual.setFechaUltimo(MatchColombiaTime.now());
        if (actor != null) {
            manual.setModificadoPor(actor.getNombre());
        }
        clasificacionManualRepository.save(manual);

        return getStandingsForGroup(team.getGrupo()).stream()
                .filter(s -> s.teamId().equals(String.valueOf(teamId)))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public StandingDTO resetManualStanding(Long teamId, Usuario actor) {
        Equipo team = equipoRepository
                .findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Equipo no encontrado"));

        clasificacionManualRepository.findById(teamId).ifPresent(m -> {
            m.setActivo(false);
            if (actor != null) {
                m.setModificadoPor(actor.getNombre());
                m.setFechaUltimo(MatchColombiaTime.now());
            }
            clasificacionManualRepository.save(m);
        });

        return getStandingsForGroup(team.getGrupo()).stream()
                .filter(s -> s.teamId().equals(String.valueOf(teamId)))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Desempate FIFA (fase de grupos):
     * 1) Puntos
     * 2) Enfrentamiento directo (mini-tabla entre empatados)
     * 3) Diferencia de goles general
     * 4) Goles a favor generales
     */
    private List<StandingDTO> sortWithTiebreakers(
            List<StandingDTO> standings, List<Partido> groupMatches, Map<Long, Resultado> resultadosByMatchId) {
        List<StandingDTO> sorted = new ArrayList<>(standings);
        sorted.sort((a, b) -> compareStandings(a, b, sorted, groupMatches, resultadosByMatchId));
        return sorted;
    }

    private int compareStandings(
            StandingDTO a,
            StandingDTO b,
            List<StandingDTO> allStandings,
            List<Partido> groupMatches,
            Map<Long, Resultado> resultadosByMatchId) {
        if (a.pts() != b.pts()) {
            return Integer.compare(b.pts(), a.pts());
        }

        Set<String> tiedIds = allStandings.stream()
                .filter(s -> s.pts() == a.pts())
                .map(StandingDTO::teamId)
                .collect(Collectors.toCollection(HashSet::new));

        if (tiedIds.size() >= 2) {
            int headToHead = compareHeadToHead(a, b, tiedIds, groupMatches, resultadosByMatchId);
            if (headToHead != 0) {
                return headToHead;
            }
        }

        if (a.dg() != b.dg()) {
            return Integer.compare(b.dg(), a.dg());
        }
        if (a.gf() != b.gf()) {
            return Integer.compare(b.gf(), b.gf());
        }
        return a.name().compareToIgnoreCase(b.name());
    }

    private int compareHeadToHead(
            StandingDTO a,
            StandingDTO b,
            Set<String> tiedTeamIds,
            List<Partido> groupMatches,
            Map<Long, Resultado> resultadosByMatchId) {
        Map<Long, MutableStats> miniStats = new HashMap<>();
        for (String id : tiedTeamIds) {
            miniStats.put(Long.parseLong(id), MutableStats.zero());
        }

        for (Partido partido : groupMatches) {
            Long homeId = partido.getEquipoLocal().getId();
            Long awayId = partido.getEquipoVisitante().getId();
            if (!tiedTeamIds.contains(String.valueOf(homeId)) || !tiedTeamIds.contains(String.valueOf(awayId))) {
                continue;
            }
            Resultado resultado = resultadosByMatchId.get(partido.getId());
            if (resultado == null) {
                continue;
            }
            applyMatch(miniStats, homeId, awayId, resultado);
        }

        MutableStats statsA = miniStats.get(Long.parseLong(a.teamId()));
        MutableStats statsB = miniStats.get(Long.parseLong(b.teamId()));
        if (statsA == null || statsB == null) {
            return 0;
        }

        if (statsA.pts != statsB.pts) {
            return Integer.compare(statsB.pts, statsA.pts);
        }
        int h2hDgA = statsA.gf - statsA.gc;
        int h2hDgB = statsB.gf - statsB.gc;
        if (h2hDgA != h2hDgB) {
            return Integer.compare(h2hDgB, h2hDgA);
        }
        if (statsA.gf != statsB.gf) {
            return Integer.compare(statsB.gf, statsA.gf);
        }
        return 0;
    }

    private static boolean isSameGroupMatch(Partido partido, String group) {
        String localGroup = partido.getEquipoLocal().getGrupo().trim().toUpperCase();
        String visitorGroup = partido.getEquipoVisitante().getGrupo().trim().toUpperCase();
        return group.equals(localGroup) && group.equals(visitorGroup);
    }

    private static void applyMatch(
            Map<Long, MutableStats> statsByTeam, Long homeId, Long awayId, Resultado resultado) {
        int gl = resultado.getGolesLocal();
        int gv = resultado.getGolesVisitante();

        MutableStats home = statsByTeam.computeIfAbsent(homeId, id -> MutableStats.zero());
        MutableStats away = statsByTeam.computeIfAbsent(awayId, id -> MutableStats.zero());

        home.pj++;
        away.pj++;
        home.gf += gl;
        home.gc += gv;
        away.gf += gv;
        away.gc += gl;

        if (gl > gv) {
            home.pg++;
            home.pts += 3;
            away.pp++;
        } else if (gv > gl) {
            away.pg++;
            away.pts += 3;
            home.pp++;
        } else {
            home.pe++;
            away.pe++;
            home.pts++;
            away.pts++;
        }
    }

    private static StandingDTO toDto(
            Equipo team,
            MutableStats stats,
            boolean provisional,
            boolean manualOverride,
            ClasificacionManual manual) {
        String modifiedBy = manual != null ? manual.getModificadoPor() : null;
        String seal = manual != null ? manual.getSelloFirma() : null;
        String modifiedAt = manual != null && manual.getFechaUltimo() != null
                ? MatchColombiaTime.toIsoOffset(manual.getFechaUltimo())
                : null;

        return new StandingDTO(
                String.valueOf(team.getId()),
                team.getNombre(),
                team.getPais(),
                team.getBanderaUrl() != null ? team.getBanderaUrl() : "",
                stats.pts,
                stats.pj,
                stats.pg,
                stats.pe,
                stats.pp,
                stats.gf,
                stats.gc,
                stats.gf - stats.gc,
                provisional,
                manualOverride,
                modifiedBy,
                seal,
                modifiedAt);
    }

    private static final class MutableStats {
        int pts;
        int pj;
        int pg;
        int pe;
        int pp;
        int gf;
        int gc;

        static MutableStats zero() {
            return new MutableStats();
        }

        static MutableStats fromManual(ClasificacionManual manual) {
            MutableStats stats = new MutableStats();
            stats.pts = manual.getPts();
            stats.pj = manual.getPj();
            stats.pg = manual.getPg();
            stats.pe = manual.getPe();
            stats.pp = manual.getPp();
            stats.gf = manual.getGf();
            stats.gc = manual.getGc();
            return stats;
        }
    }
}
