package com.futprediction.match.service;

import com.futprediction.auth.entity.Usuario;
import com.futprediction.match.dto.CreateMatchEventRequestDTO;
import com.futprediction.match.dto.MatchCreateRequestDTO;
import com.futprediction.match.dto.MatchEventViewDTO;
import com.futprediction.match.dto.MatchResponseDTO;
import com.futprediction.match.dto.MatchViewDTO;
import com.futprediction.match.dto.OfficializeMatchRequestDTO;
import com.futprediction.match.dto.UpdateLiveClockRequestDTO;
import com.futprediction.match.dto.UpdateLiveScoreRequestDTO;
import com.futprediction.match.entity.EventoPartido;
import com.futprediction.match.entity.Partido;
import com.futprediction.match.entity.Resultado;
import com.futprediction.match.repository.EventoPartidoRepository;
import com.futprediction.match.repository.PartidoRepository;
import com.futprediction.match.repository.ResultadoRepository;
import com.futprediction.match.service.rules.MatchSchedulingRule;
import com.futprediction.match.support.MatchColombiaTime;
import com.futprediction.teams.entity.Equipo;
import com.futprediction.teams.repository.EquipoRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MatchService {

    private final PartidoRepository partidoRepository;
    private final EquipoRepository equipoRepository;
    private final ResultadoRepository resultadoRepository;
    private final EventoPartidoRepository eventoPartidoRepository;
    private final List<MatchSchedulingRule> matchSchedulingRules;

    public MatchService(
            PartidoRepository partidoRepository,
            EquipoRepository equipoRepository,
            ResultadoRepository resultadoRepository,
            EventoPartidoRepository eventoPartidoRepository,
            List<MatchSchedulingRule> matchSchedulingRules) {
        this.partidoRepository = partidoRepository;
        this.equipoRepository = equipoRepository;
        this.resultadoRepository = resultadoRepository;
        this.eventoPartidoRepository = eventoPartidoRepository;
        this.matchSchedulingRules = matchSchedulingRules;
    }

    public List<MatchViewDTO> listMatchesForUi() {
        List<Partido> partidos = partidoRepository.findAllWithEquipos();
        if (partidos.isEmpty()) {
            return List.of();
        }

        List<Long> ids = partidos.stream().map(Partido::getId).toList();
        Map<Long, Resultado> resultadosByMatchId = resultadoRepository.findByPartido_IdIn(ids).stream()
                .collect(Collectors.toMap(r -> r.getPartido().getId(), r -> r));
        Map<Long, List<EventoPartido>> eventsByMatchId = eventoPartidoRepository.findByPartido_IdIn(ids).stream()
                .collect(Collectors.groupingBy(e -> e.getPartido().getId()));

        return partidos.stream()
                .map(p -> toMatchView(
                        p,
                        resultadosByMatchId.get(p.getId()),
                        eventsByMatchId.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    public List<MatchViewDTO> listLiveMatchesForUi() {
        return listMatchesForUi().stream()
                .filter(m -> "EN_VIVO".equals(m.status()))
                .toList();
    }

    public MatchViewDTO getMatchViewById(Long id) {
        Partido partido = partidoRepository
                .findWithEquiposById(id)
                .orElseThrow(() -> new EntityNotFoundException("Partido no encontrado"));
        return toMatchView(partido, loadResultado(partido.getId()), loadEvents(partido.getId()));
    }

    private Resultado loadResultado(Long partidoId) {
        return resultadoRepository.findByPartido_Id(partidoId).orElse(null);
    }

    private MatchViewDTO toMatchView(Partido partido) {
        return toMatchView(partido, loadResultado(partido.getId()), loadEvents(partido.getId()));
    }

    private MatchViewDTO toMatchView(Partido partido, Resultado resultado, List<EventoPartido> eventos) {
        int golesLocal = resultado != null ? resultado.getGolesLocal() : 0;
        int golesVisitante = resultado != null ? resultado.getGolesVisitante() : 0;
        Equipo local = partido.getEquipoLocal();
        Equipo visitante = partido.getEquipoVisitante();
        return new MatchViewDTO(
                String.valueOf(partido.getId()),
                local.getNombre(),
                visitante.getNombre(),
                local.getPais(),
                visitante.getPais(),
                local.getBanderaUrl() != null ? local.getBanderaUrl() : "",
                visitante.getBanderaUrl() != null ? visitante.getBanderaUrl() : "",
                golesLocal,
                golesVisitante,
                mapEstado(partido.getEstado()),
                formatDate(partido.getFecha()),
                partido.getEstadio(),
                partido.getFase(),
                computeLiveMinute(partido),
                stoppageTime(partido),
                isHalftimeBreak(partido),
                mapEvents(eventos),
                partido.getProgramadoPor() != null ? partido.getProgramadoPor() : "");
    }

    private List<EventoPartido> loadEvents(Long partidoId) {
        return eventoPartidoRepository.findByPartido_IdOrderByMinutoDescIdDesc(partidoId);
    }

    private List<MatchEventViewDTO> mapEvents(List<EventoPartido> eventos) {
        return eventos.stream()
                .sorted(Comparator.comparing(EventoPartido::getMinuto)
                        .reversed()
                        .thenComparing(EventoPartido::getId, Comparator.reverseOrder()))
                .map(e -> new MatchEventViewDTO(
                        String.valueOf(e.getId()),
                        e.getMinuto(),
                        mapEventTypeToUi(e.getTipoEvento()),
                        e.getDescripcion()))
                .toList();
    }

    private static String mapEventTypeToUi(EventoPartido.TipoEvento tipo) {
        return switch (tipo) {
            case GOAL -> "goal";
            case YELLOW_CARD -> "yellow";
            case RED_CARD -> "red";
        };
    }

    private static EventoPartido.TipoEvento mapEventTypeFromRequest(String type) {
        return switch (type.toLowerCase()) {
            case "goal" -> EventoPartido.TipoEvento.GOAL;
            case "yellow" -> EventoPartido.TipoEvento.YELLOW_CARD;
            case "red" -> EventoPartido.TipoEvento.RED_CARD;
            default -> throw new IllegalArgumentException("Tipo de evento no válido: " + type);
        };
    }

    private int computeLiveMinute(Partido partido) {
        if (partido.getEstado() == Partido.EstadoPartido.SUSPENDIDO) {
            return partido.getMinutoSuspendido() != null ? partido.getMinutoSuspendido() : 0;
        }
        if (partido.getEstado() != Partido.EstadoPartido.EN_CURSO) {
            return 0;
        }
        if (Boolean.TRUE.equals(partido.getEnDescanso()) && partido.getMinutoCongelado() != null) {
            return partido.getMinutoCongelado();
        }

        LocalDateTime now = MatchColombiaTime.now();
        if (partido.getInicioSegundoTiempo() != null) {
            long elapsed = Duration.between(partido.getInicioSegundoTiempo(), now).toMinutes();
            return (int) Math.max(45, 45 + Math.max(0, elapsed));
        }

        LocalDateTime kickoff =
                partido.getInicioEnVivo() != null ? partido.getInicioEnVivo() : partido.getFecha();
        if (kickoff.isAfter(now)) {
            return 0;
        }
        return (int) Math.max(0, Duration.between(kickoff, now).toMinutes());
    }

    private static int stoppageTime(Partido partido) {
        return partido.getTiempoAdicion() != null ? Math.max(0, partido.getTiempoAdicion()) : 0;
    }

    private static boolean isHalftimeBreak(Partido partido) {
        return Boolean.TRUE.equals(partido.getEnDescanso());
    }

    private void syncKickoffToMinute(Partido partido, int minute) {
        LocalDateTime now = MatchColombiaTime.now();
        if (partido.getInicioSegundoTiempo() != null) {
            int secondHalfElapsed = Math.max(0, minute - 45);
            partido.setInicioSegundoTiempo(now.minusMinutes(secondHalfElapsed));
        } else {
            partido.setInicioEnVivo(now.minusMinutes(minute));
        }
    }

    private void restoreLiveClockAfterSuspend(Partido partido, int frozenMinute) {
        LocalDateTime now = MatchColombiaTime.now();
        if (frozenMinute > 45 || partido.getInicioSegundoTiempo() != null) {
            partido.setInicioSegundoTiempo(now.minusMinutes(Math.max(0, frozenMinute - 45)));
        } else {
            partido.setInicioEnVivo(now.minusMinutes(frozenMinute));
            partido.setInicioSegundoTiempo(null);
        }
    }

    private static String mapEstado(Partido.EstadoPartido estado) {
        return switch (estado) {
            case PROGRAMADO -> "PROGRAMADO";
            case EN_CURSO -> "EN_VIVO";
            case SUSPENDIDO -> "SUSPENDIDO";
            case FINALIZADO -> "OFICIAL";
        };
    }

    private static String formatDate(LocalDateTime fecha) {
        return fecha.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public MatchResponseDTO scheduleMatch(MatchCreateRequestDTO request, Usuario actor) {
        for (MatchSchedulingRule rule : matchSchedulingRules) {
            rule.validate(request);
        }

        Equipo local = equipoRepository.findById(request.idEquipoLocal())
                .orElseThrow(() -> new EntityNotFoundException("Equipo local no encontrado"));
        Equipo visitante = equipoRepository.findById(request.idEquipoVisitante())
                .orElseThrow(() -> new EntityNotFoundException("Equipo visitante no encontrado"));

        Partido partido = new Partido();
        partido.setEquipoLocal(local);
        partido.setEquipoVisitante(visitante);
        partido.setFecha(request.fechaHora());
        partido.setEstadio(request.estadio());
        partido.setFase(request.fase());
        partido.setEstado(Partido.EstadoPartido.PROGRAMADO);
        if (actor != null) {
            partido.setProgramadoPor(actor.getNombre());
        }

        Partido saved = partidoRepository.save(partido);

        return new MatchResponseDTO(
                saved.getId(),
                saved.getEquipoLocal().getId(),
                saved.getEquipoVisitante().getId(),
                saved.getFecha(),
                saved.getEstadio(),
                saved.getFase(),
                saved.getEstado().name()
        );
    }

    @Transactional
    public int activateDueMatches() {
        LocalDateTime ahora = MatchColombiaTime.now();
        List<Partido> listos = partidoRepository.findProgramadosListosParaIniciar(
                Partido.EstadoPartido.PROGRAMADO, ahora);
        for (Partido partido : listos) {
            transitionToLive(partido);
        }
        return listos.size();
    }

    @Transactional
    public MatchViewDTO startMatchLive(Long partidoId, Usuario actor) {
        Partido partido = partidoRepository
                .findById(partidoId)
                .orElseThrow(() -> new EntityNotFoundException("Partido no encontrado"));

        if (partido.getEstado() == Partido.EstadoPartido.FINALIZADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El partido ya fue oficializado");
        }
        if (partido.getEstado() == Partido.EstadoPartido.EN_CURSO) {
            return getMatchViewById(partidoId);
        }
        if (partido.getEstado() == Partido.EstadoPartido.SUSPENDIDO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El partido está suspendido. Usa Reanudar en lugar de Iniciar ya.");
        }
        if (partido.getEstado() != Partido.EstadoPartido.PROGRAMADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede iniciar este partido");
        }

        transitionToLive(partido);
        return getMatchViewById(partidoId);
    }

    @Transactional
    public MatchViewDTO suspendMatch(Long partidoId, Usuario actor) {
        Partido partido = partidoRepository
                .findById(partidoId)
                .orElseThrow(() -> new EntityNotFoundException("Partido no encontrado"));

        if (partido.getEstado() != Partido.EstadoPartido.EN_CURSO
                && partido.getEstado() != Partido.EstadoPartido.PROGRAMADO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Solo se pueden suspender partidos programados o en vivo");
        }

        if (partido.getEstado() == Partido.EstadoPartido.EN_CURSO) {
            partido.setMinutoSuspendido(computeLiveMinute(partido));
            partido.setEnDescanso(false);
        } else {
            partido.setMinutoSuspendido(0);
        }
        partido.setEstado(Partido.EstadoPartido.SUSPENDIDO);
        partidoRepository.save(partido);
        return getMatchViewById(partidoId);
    }

    @Transactional
    public MatchViewDTO resumeMatch(Long partidoId, Usuario actor) {
        Partido partido = partidoRepository
                .findById(partidoId)
                .orElseThrow(() -> new EntityNotFoundException("Partido no encontrado"));

        if (partido.getEstado() != Partido.EstadoPartido.SUSPENDIDO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Solo se pueden reanudar partidos suspendidos");
        }

        if (partido.getInicioEnVivo() == null) {
            partido.setMinutoSuspendido(null);
            partido.setEstado(Partido.EstadoPartido.PROGRAMADO);
        } else {
            int frozenMinute = partido.getMinutoSuspendido() != null ? partido.getMinutoSuspendido() : 0;
            restoreLiveClockAfterSuspend(partido, frozenMinute);
            partido.setMinutoSuspendido(null);
            partido.setMinutoCongelado(null);
            partido.setEnDescanso(false);
            partido.setEstado(Partido.EstadoPartido.EN_CURSO);
        }
        partidoRepository.save(partido);
        return getMatchViewById(partidoId);
    }

    @Transactional
    public MatchViewDTO updateLiveClock(Long partidoId, UpdateLiveClockRequestDTO request, Usuario actor) {
        Partido partido = partidoRepository
                .findById(partidoId)
                .orElseThrow(() -> new EntityNotFoundException("Partido no encontrado"));

        if (partido.getEstado() != Partido.EstadoPartido.EN_CURSO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Solo se puede ajustar el reloj de partidos en vivo");
        }

        partido.setTiempoAdicion(request.stoppageTime());
        if (Boolean.TRUE.equals(partido.getEnDescanso())) {
            partido.setMinutoCongelado(request.minute());
        } else {
            syncKickoffToMinute(partido, request.minute());
        }
        partidoRepository.save(partido);
        return getMatchViewById(partidoId);
    }

    @Transactional
    public MatchViewDTO enterHalftime(Long partidoId, Usuario actor) {
        Partido partido = partidoRepository
                .findById(partidoId)
                .orElseThrow(() -> new EntityNotFoundException("Partido no encontrado"));

        if (partido.getEstado() != Partido.EstadoPartido.EN_CURSO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Solo se puede marcar descanso en partidos en vivo");
        }
        if (Boolean.TRUE.equals(partido.getEnDescanso())) {
            return getMatchViewById(partidoId);
        }

        partido.setEnDescanso(true);
        partido.setMinutoCongelado(computeLiveMinute(partido));
        partido.setTiempoAdicion(0);
        partidoRepository.save(partido);
        return getMatchViewById(partidoId);
    }

    @Transactional
    public MatchViewDTO resumeAfterHalftime(Long partidoId, Usuario actor) {
        Partido partido = partidoRepository
                .findById(partidoId)
                .orElseThrow(() -> new EntityNotFoundException("Partido no encontrado"));

        if (partido.getEstado() != Partido.EstadoPartido.EN_CURSO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Solo se puede reanudar partidos en vivo");
        }
        if (!Boolean.TRUE.equals(partido.getEnDescanso())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El partido no está en descanso");
        }

        partido.setEnDescanso(false);
        partido.setMinutoCongelado(null);
        partido.setTiempoAdicion(0);
        partido.setInicioSegundoTiempo(MatchColombiaTime.now());
        partidoRepository.save(partido);
        return getMatchViewById(partidoId);
    }

    @Transactional
    public MatchViewDTO updateLiveScore(Long partidoId, UpdateLiveScoreRequestDTO request, Usuario actor) {
        Partido partido = partidoRepository
                .findById(partidoId)
                .orElseThrow(() -> new EntityNotFoundException("Partido no encontrado"));

        if (partido.getEstado() != Partido.EstadoPartido.EN_CURSO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Solo se puede actualizar el marcador de partidos en vivo");
        }

        Resultado resultado = ensureResultado(partido);
        resultado.setGolesLocal(request.localScore());
        resultado.setGolesVisitante(request.visitorScore());
        resultado.setFechaRegistro(MatchColombiaTime.now());
        resultadoRepository.save(resultado);

        return getMatchViewById(partidoId);
    }

    @Transactional
    public MatchViewDTO addMatchEvent(Long partidoId, CreateMatchEventRequestDTO request, Usuario actor) {
        Partido partido = partidoRepository
                .findWithEquiposById(partidoId)
                .orElseThrow(() -> new EntityNotFoundException("Partido no encontrado"));

        if (partido.getEstado() != Partido.EstadoPartido.EN_CURSO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Solo se pueden registrar eventos en partidos en vivo");
        }

        EventoPartido.TipoEvento tipo = mapEventTypeFromRequest(request.type());
        String teamName =
                "local".equalsIgnoreCase(request.teamSide())
                        ? partido.getEquipoLocal().getNombre()
                        : partido.getEquipoVisitante().getNombre();
        String label = switch (tipo) {
            case GOAL -> "Gol";
            case YELLOW_CARD -> "Tarjeta amarilla";
            case RED_CARD -> "Tarjeta roja";
        };
        String description = request.playerName().trim() + " (" + teamName + ") — " + label;

        EventoPartido evento = new EventoPartido();
        evento.setPartido(partido);
        evento.setTipoEvento(tipo);
        evento.setMinuto(request.minute());
        evento.setDescripcion(description);
        eventoPartidoRepository.save(evento);

        if (tipo == EventoPartido.TipoEvento.GOAL) {
            Resultado resultado = ensureResultado(partido);
            if ("local".equalsIgnoreCase(request.teamSide())) {
                resultado.setGolesLocal(resultado.getGolesLocal() + 1);
            } else {
                resultado.setGolesVisitante(resultado.getGolesVisitante() + 1);
            }
            resultado.setFechaRegistro(MatchColombiaTime.now());
            resultadoRepository.save(resultado);
        }

        return getMatchViewById(partidoId);
    }

    @Transactional
    public MatchViewDTO deleteMatchEvent(Long partidoId, Long eventoId, Usuario actor) {
        Partido partido = partidoRepository
                .findById(partidoId)
                .orElseThrow(() -> new EntityNotFoundException("Partido no encontrado"));

        if (partido.getEstado() != Partido.EstadoPartido.EN_CURSO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Solo se pueden eliminar eventos de partidos en vivo");
        }

        EventoPartido evento = eventoPartidoRepository
                .findById(eventoId)
                .orElseThrow(() -> new EntityNotFoundException("Evento no encontrado"));

        if (!evento.getPartido().getId().equals(partidoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El evento no pertenece a este partido");
        }

        if (evento.getTipoEvento() == EventoPartido.TipoEvento.GOAL) {
            Resultado resultado = ensureResultado(partido);
            String desc = evento.getDescripcion() != null ? evento.getDescripcion() : "";
            if (desc.contains("(" + partido.getEquipoLocal().getNombre() + ")")) {
                resultado.setGolesLocal(Math.max(0, resultado.getGolesLocal() - 1));
            } else {
                resultado.setGolesVisitante(Math.max(0, resultado.getGolesVisitante() - 1));
            }
            resultadoRepository.save(resultado);
        }

        eventoPartidoRepository.delete(evento);
        return getMatchViewById(partidoId);
    }

    private void transitionToLive(Partido partido) {
        partido.setInicioEnVivo(MatchColombiaTime.now());
        partido.setInicioSegundoTiempo(null);
        partido.setMinutoSuspendido(null);
        partido.setMinutoCongelado(null);
        partido.setEnDescanso(false);
        partido.setTiempoAdicion(0);
        partido.setEstado(Partido.EstadoPartido.EN_CURSO);
        partidoRepository.save(partido);
        Resultado resultado = ensureResultado(partido);
        resultado.setGolesLocal(0);
        resultado.setGolesVisitante(0);
        resultadoRepository.save(resultado);
    }

    private Resultado ensureResultado(Partido partido) {
        return resultadoRepository
                .findByPartido_Id(partido.getId())
                .orElseGet(() -> {
                    Resultado nuevo = new Resultado();
                    nuevo.setPartido(partido);
                    nuevo.setGolesLocal(0);
                    nuevo.setGolesVisitante(0);
                    nuevo.setFechaRegistro(MatchColombiaTime.now());
                    return nuevo;
                });
    }

    @Transactional
    public MatchViewDTO officializeResult(Long partidoId, OfficializeMatchRequestDTO request, Usuario actor) {
        Partido partido = partidoRepository
                .findById(partidoId)
                .orElseThrow(() -> new EntityNotFoundException("Partido no encontrado"));

        if (partido.getEstado() == Partido.EstadoPartido.FINALIZADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El partido ya esta oficializado");
        }

        if (partido.getEstado() != Partido.EstadoPartido.EN_CURSO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se puede oficializar un partido que está en vivo. Inicia el partido o espera la hora programada.");
        }

        Resultado resultado = ensureResultado(partido);
        resultado.setGolesLocal(request.localScore());
        resultado.setGolesVisitante(request.visitorScore());
        resultado.setFechaRegistro(MatchColombiaTime.now());
        resultadoRepository.save(resultado);

        partido.setEstado(Partido.EstadoPartido.FINALIZADO);
        partidoRepository.save(partido);

        return getMatchViewById(partidoId);
    }

    public MatchViewDTO updateMatch(Long partidoId, MatchCreateRequestDTO request) {
        Partido partido = partidoRepository
                .findWithEquiposById(partidoId)
                .orElseThrow(() -> new EntityNotFoundException("Partido no encontrado"));

        if (partido.getEstado() != Partido.EstadoPartido.PROGRAMADO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Solo se pueden editar partidos en estado PROGRAMADO");
        }

        for (MatchSchedulingRule rule : matchSchedulingRules) {
            rule.validate(request);
        }

        Equipo local = equipoRepository.findById(request.idEquipoLocal())
                .orElseThrow(() -> new EntityNotFoundException("Equipo local no encontrado"));
        Equipo visitante = equipoRepository.findById(request.idEquipoVisitante())
                .orElseThrow(() -> new EntityNotFoundException("Equipo visitante no encontrado"));

        partido.setEquipoLocal(local);
        partido.setEquipoVisitante(visitante);
        partido.setFecha(request.fechaHora());
        partido.setEstadio(request.estadio());
        partido.setFase(request.fase());
        partidoRepository.save(partido);

        return getMatchViewById(partidoId);
    }

    @Transactional
    public void deleteMatch(Long partidoId) {
        Partido partido = partidoRepository
                .findById(partidoId)
                .orElseThrow(() -> new EntityNotFoundException("Partido no encontrado"));

        if (partido.getEstado() == Partido.EstadoPartido.FINALIZADO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "No se puede eliminar un partido ya oficializado");
        }

        resultadoRepository.findByPartido_Id(partidoId).ifPresent(resultadoRepository::delete);
        eventoPartidoRepository.deleteByPartido_Id(partidoId);
        partidoRepository.delete(partido);
    }
}
