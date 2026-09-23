package com.futprediction.prediction.service;

import com.futprediction.auth.entity.Usuario;
import com.futprediction.auth.repository.UsuarioRepository;
import com.futprediction.match.entity.Partido;
import com.futprediction.match.entity.Resultado;
import com.futprediction.match.repository.PartidoRepository;
import com.futprediction.match.repository.ResultadoRepository;
import com.futprediction.admin.dto.AdminPredictionViewDTO;
import com.futprediction.prediction.client.PredictionLlmClient;
import com.futprediction.prediction.client.dto.LlmPredictionResponse;
import com.futprediction.prediction.config.PredictionLlmProperties;
import com.futprediction.prediction.dto.PredictionHistoryDTO;
import com.futprediction.prediction.dto.PredictionResultDTO;
import com.futprediction.prediction.dto.PredictionResultDTO.H2HComparisonDTO;
import com.futprediction.prediction.dto.PredictionResultDTO.PredictionFactorDTO;
import com.futprediction.prediction.dto.PredictionStatsDTO;
import com.futprediction.prediction.dto.SavePredictionRequestDTO;
import com.futprediction.prediction.entity.Prediccion;
import com.futprediction.prediction.repository.PrediccionRepository;
import com.futprediction.teams.entity.Equipo;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PredictionIntegrationService {

    private final PredictionLlmClient llmClient;
    private final PredictionLlmProperties llmProperties;
    private final PartidoRepository partidoRepository;
    private final ResultadoRepository resultadoRepository;
    private final PrediccionRepository prediccionRepository;
    private final UsuarioRepository usuarioRepository;

    public PredictionIntegrationService(
            PredictionLlmClient llmClient,
            PredictionLlmProperties llmProperties,
            PartidoRepository partidoRepository,
            ResultadoRepository resultadoRepository,
            PrediccionRepository prediccionRepository,
            UsuarioRepository usuarioRepository) {
        this.llmClient = llmClient;
        this.llmProperties = llmProperties;
        this.partidoRepository = partidoRepository;
        this.resultadoRepository = resultadoRepository;
        this.prediccionRepository = prediccionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public PredictionResultDTO generateForMatch(Long matchId) {
        Partido partido = partidoRepository
                .findWithEquiposById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Partido no encontrado"));

        Equipo local = partido.getEquipoLocal();
        Equipo visitante = partido.getEquipoVisitante();
        String home = local.getNombre();
        String away = visitante.getNombre();

        if (llmProperties.enabled()) {
            try {
                LlmPredictionResponse llm = llmClient.generatePrediction(
                        home,
                        local.getPais(),
                        away,
                        visitante.getPais(),
                        local.getRankingFifa(),
                        visitante.getRankingFifa(),
                        partido.getFase(),
                        partido.getEstadio(),
                        local.getGrupo(),
                        visitante.getGrupo());
                return mapLlmToResult(String.valueOf(matchId), partido, llm);
            } catch (IllegalStateException ex) {
                return buildLocalFallbackResult(String.valueOf(matchId), partido, ex.getMessage());
            }
        }
        return buildLocalFallbackResult(String.valueOf(matchId), partido, "Servicio LLM deshabilitado");
    }

    public List<AdminPredictionViewDTO> listAllForAdmin() {
        return prediccionRepository.findAllWithDetails().stream()
                .map(p -> {
                    String predicted = resolvePredictedLabel(p);
                    return new AdminPredictionViewDTO(
                            String.valueOf(p.getId()),
                            p.getUsuario().getNombre(),
                            p.getUsuario().getEmail(),
                            p.getPartido().getEquipoLocal().getNombre() + " vs "
                                    + p.getPartido().getEquipoVisitante().getNombre(),
                            predicted,
                            resolveOfficialResult(p.getPartido()),
                            resolveStatus(p.getPartido(), predicted),
                            formatInstant(p.getPartido().getFecha()),
                            parseConfidence(p.getNivelConfianza()));
                })
                .toList();
    }

    public SavePredictionRequestDTO buildSaveRequestFromResult(PredictionResultDTO result) {
        String predicted = result.predictedWinner() != null
                ? "Gana " + result.predictedWinner()
                : resolvePredictedLabelFromProbabilities(result);
        return new SavePredictionRequestDTO(
                result.matchId(),
                predicted,
                result.confidence(),
                result.justification(),
                result.localWinProbability(),
                result.drawProbability(),
                result.visitorWinProbability(),
                result.predictedLocalScore(),
                result.predictedVisitorScore(),
                result.modelVersion());
    }

    @Transactional
    public PredictionHistoryDTO savePrediction(SavePredictionRequestDTO request, Usuario usuario) {
        Long matchId = Long.parseLong(request.matchId());
        Partido partido = partidoRepository
                .findWithEquiposById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Partido no encontrado"));

        Prediccion prediccion = prediccionRepository
                .findByUsuario_IdAndPartido_Id(usuario.getId(), matchId)
                .orElseGet(Prediccion::new);

        prediccion.setUsuario(usuarioRepository.getReferenceById(usuario.getId()));
        prediccion.setPartido(partidoRepository.getReferenceById(matchId));

        double probLocal = toFraction(request.localWinProbability(), 0.33);
        double probEmpate = toFraction(request.drawProbability(), 0.34);
        double probVisitante = toFraction(request.visitorWinProbability(), 0.33);
        double sum = probLocal + probEmpate + probVisitante;
        if (sum > 0) {
            probLocal /= sum;
            probEmpate /= sum;
            probVisitante /= sum;
        }

        prediccion.setProbLocal(toDecimal(probLocal));
        prediccion.setProbEmpate(toDecimal(probEmpate));
        prediccion.setProbVisitante(toDecimal(probVisitante));
        prediccion.setExplicacion(truncateExplanation(request.justification()));
        prediccion.setNivelConfianza(formatConfidence(request.accuracy()));
        prediccion.setGolesLocalPrevisto(request.predictedLocalScore());
        prediccion.setGolesVisitantePrevisto(request.predictedVisitorScore());
        prediccion.setModeloVersion(request.modelVersion());
        prediccion.setFechaPrediccion(java.time.LocalDateTime.now());

        Prediccion saved = prediccionRepository.save(prediccion);
        return toHistoryDto(saved, partido, request.predictedResult());
    }

    public List<PredictionHistoryDTO> getHistory(Usuario usuario) {
        return prediccionRepository.findByUsuarioIdWithPartido(usuario.getId()).stream()
                .map(p -> toHistoryDto(p, p.getPartido(), resolvePredictedLabel(p)))
                .toList();
    }

    public PredictionStatsDTO getStats(Usuario usuario) {
        List<PredictionHistoryDTO> history = getHistory(usuario);
        int total = history.size();
        int correct = (int) history.stream().filter(h -> "ACERTADA".equals(h.status())).count();
        int failed = (int) history.stream().filter(h -> "FALLIDA".equals(h.status())).count();
        int pending = (int) history.stream().filter(h -> "PENDIENTE".equals(h.status())).count();
        int finished = correct + failed;
        double accuracyRate = finished == 0 ? 0 : Math.round((correct * 1000.0) / finished) / 10.0;
        return new PredictionStatsDTO(total, correct, failed, pending, accuracyRate);
    }

    @Transactional
    public void deletePrediction(Long predictionId, Usuario usuario) {
        Prediccion prediccion = prediccionRepository
                .findByIdAndUsuarioIdWithPartido(predictionId, usuario.getId())
                .orElseThrow(() -> new EntityNotFoundException("Predicción no encontrada"));
        prediccionRepository.delete(prediccion);
    }

    private PredictionResultDTO mapLlmToResult(String matchId, Partido partido, LlmPredictionResponse llm) {
        Equipo local = partido.getEquipoLocal();
        Equipo visitante = partido.getEquipoVisitante();
        RankingProbabilities ranking = computeRankingProbabilities(local, visitante);

        boolean trustDeepSeek = llm.modelVersion() != null && llm.modelVersion().toLowerCase().startsWith("deepseek");
        boolean useRanking = !trustDeepSeek && isUnreliableLlmOutput(llm);
        double homePct = useRanking ? ranking.homePct() : normalizeLlmPercent(llm.homeWinProbability(), 33.0);
        double drawPct = useRanking ? ranking.drawPct() : normalizeLlmPercent(llm.drawProbability(), 10.0);
        double awayPct = useRanking ? ranking.awayPct() : normalizeLlmPercent(llm.awayWinProbability(), 33.0);

        if (!useRanking) {
            double total = homePct + drawPct + awayPct;
            if (total > 0) {
                homePct = round1(homePct * 100 / total);
                drawPct = round1(drawPct * 100 / total);
                awayPct = round1(awayPct * 100 / total);
            }
        }

        String predictedWinner = resolveWinnerName(local, visitante, homePct, drawPct, awayPct);
        double confidence = round1(Math.max(homePct, Math.max(drawPct, awayPct)));
        String justification = useRanking
                ? buildSpanishJustification(partido, local, visitante, ranking, predictedWinner)
                : (llm.reasoning() != null ? llm.reasoning() : buildSpanishJustification(
                        partido, local, visitante, ranking, predictedWinner));
        String modelVersion = useRanking ? "fifa-ranking-es" : llm.modelVersion();
        Integer predictedLocalScore = trustDeepSeek ? llm.predictedHomeScore() : null;
        Integer predictedVisitorScore = trustDeepSeek ? llm.predictedAwayScore() : null;

        List<PredictionFactorDTO> factors = new ArrayList<>();
        factors.add(new PredictionFactorDTO(
                "positive",
                "Ranking FIFA: " + local.getNombre() + " (#" + local.getRankingFifa() + ") vs "
                        + visitante.getNombre() + " (#" + visitante.getRankingFifa() + ")"));
        factors.add(new PredictionFactorDTO(
                "neutral",
                useRanking
                        ? "Modelo: ranking FIFA + ventaja local (~10%)"
                        : "Modelo: " + (llm.modelVersion() != null ? llm.modelVersion() : "LLM")));
        factors.add(new PredictionFactorDTO("positive", "Favorito según análisis: " + predictedWinner));

        H2HComparisonDTO h2h = new H2HComparisonDTO(
                local.getRankingFifa(),
                visitante.getRankingFifa(),
                homePct / 100.0,
                awayPct / 100.0,
                0,
                0);

        return new PredictionResultDTO(
                matchId,
                homePct,
                drawPct,
                awayPct,
                justification,
                confidence,
                modelVersion,
                "Empate".equals(predictedWinner) ? null : predictedWinner,
                predictedLocalScore,
                predictedVisitorScore,
                factors,
                h2h);
    }

    private PredictionHistoryDTO toHistoryDto(Prediccion prediccion, Partido partido, String predictedLabel) {
        String matchLabel = partido.getEquipoLocal().getNombre() + " vs " + partido.getEquipoVisitante().getNombre();
        String resultLabel = resolveOfficialResult(partido);
        String status = resolveStatus(partido, predictedLabel);

        double accuracy = 0;
        if (prediccion.getNivelConfianza() != null) {
            String digits = prediccion.getNivelConfianza().replaceAll("[^0-9.]", "");
            if (!digits.isBlank()) {
                accuracy = Double.parseDouble(digits);
            }
        }

        return new PredictionHistoryDTO(
                String.valueOf(prediccion.getId()),
                matchLabel,
                partido.getEquipoLocal().getNombre(),
                partido.getEquipoVisitante().getNombre(),
                predictedLabel,
                resultLabel,
                status,
                formatInstant(partido.getFecha()),
                accuracy,
                prediccion.getExplicacion(),
                probToPercent(prediccion.getProbLocal()),
                probToPercent(prediccion.getProbEmpate()),
                probToPercent(prediccion.getProbVisitante()),
                prediccion.getGolesLocalPrevisto(),
                prediccion.getGolesVisitantePrevisto(),
                prediccion.getModeloVersion());
    }

    private static Double probToPercent(BigDecimal prob) {
        if (prob == null) {
            return null;
        }
        return round1(prob.doubleValue() * 100);
    }

    private static String formatInstant(LocalDateTime dateTime) {
        return dateTime.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private String resolvePredictedLabelFromProbabilities(PredictionResultDTO result) {
        if (result.localWinProbability() >= result.drawProbability()
                && result.localWinProbability() >= result.visitorWinProbability()) {
            return "Gana local";
        }
        if (result.visitorWinProbability() >= result.drawProbability()) {
            return "Gana visitante";
        }
        return "Empate";
    }

    private String resolvePredictedLabel(Prediccion prediccion) {
        Partido partido = prediccion.getPartido();
        double pLocal = prediccion.getProbLocal().doubleValue();
        double pEmpate = prediccion.getProbEmpate().doubleValue();
        double pVisitante = prediccion.getProbVisitante().doubleValue();

        if (pLocal >= pEmpate && pLocal >= pVisitante) {
            return "Gana " + partido.getEquipoLocal().getNombre();
        }
        if (pVisitante >= pEmpate) {
            return "Gana " + partido.getEquipoVisitante().getNombre();
        }
        return "Empate";
    }

    private String resolveOfficialResult(Partido partido) {
        if (partido.getEstado() != Partido.EstadoPartido.FINALIZADO) {
            return "—";
        }
        return resultadoRepository
                .findByPartido_Id(partido.getId())
                .map(r -> r.getGolesLocal() + " - " + r.getGolesVisitante())
                .orElse("—");
    }

    private String resolveStatus(Partido partido, String predictedLabel) {
        if (partido.getEstado() != Partido.EstadoPartido.FINALIZADO) {
            return "PENDIENTE";
        }
        Resultado resultado =
                resultadoRepository.findByPartido_Id(partido.getId()).orElse(null);
        if (resultado == null) {
            return "PENDIENTE";
        }

        int gl = resultado.getGolesLocal();
        int gv = resultado.getGolesVisitante();
        String actual;
        if (gl > gv) {
            actual = "Gana " + partido.getEquipoLocal().getNombre();
        } else if (gv > gl) {
            actual = "Gana " + partido.getEquipoVisitante().getNombre();
        } else {
            actual = "Empate";
        }
        return predictedLabel.equalsIgnoreCase(actual) ? "ACERTADA" : "FALLIDA";
    }

    private static BigDecimal toDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private PredictionResultDTO buildLocalFallbackResult(String matchId, Partido partido, String llmError) {
        Equipo local = partido.getEquipoLocal();
        Equipo visitante = partido.getEquipoVisitante();
        RankingProbabilities ranking = computeRankingProbabilities(local, visitante);
        String predictedWinner = resolveWinnerName(
                local, visitante, ranking.homePct(), ranking.drawPct(), ranking.awayPct());

        List<PredictionFactorDTO> factors = new ArrayList<>();
        factors.add(new PredictionFactorDTO(
                "neutral",
                "Modo respaldo: ranking FIFA (servicio LLM no disponible)."));
        factors.add(new PredictionFactorDTO(
                "positive",
                local.getNombre() + " #" + local.getRankingFifa() + " vs " + visitante.getNombre()
                        + " #" + visitante.getRankingFifa()));

        String justification = buildSpanishJustification(partido, local, visitante, ranking, predictedWinner);
        if (llmError != null && !llmError.isBlank()) {
            justification = justification + "\n\nNota técnica: " + llmError;
        }

        return new PredictionResultDTO(
                matchId,
                ranking.homePct(),
                ranking.drawPct(),
                ranking.awayPct(),
                justification,
                ranking.confidence(),
                "local-fallback",
                "Empate".equals(predictedWinner) ? null : predictedWinner,
                null,
                null,
                factors,
                new H2HComparisonDTO(
                        local.getRankingFifa(),
                        visitante.getRankingFifa(),
                        ranking.homePct() / 100.0,
                        ranking.awayPct() / 100.0,
                        0,
                        0));
    }

    private record RankingProbabilities(
            double homePct, double drawPct, double awayPct, double confidence) {}

    private RankingProbabilities computeRankingProbabilities(Equipo local, Equipo visitante) {
        int rankLocal = Math.max(1, local.getRankingFifa());
        int rankVisitor = Math.max(1, visitante.getRankingFifa());

        double homeStrength = (1.0 / rankLocal) * 1.10;
        double awayStrength = 1.0 / rankVisitor;
        int rankDiff = Math.abs(rankLocal - rankVisitor);

        double drawPct = Math.max(15.0, Math.min(30.0, 28.0 - rankDiff * 0.35));
        double winPool = 100.0 - drawPct;
        double strengthSum = homeStrength + awayStrength;
        double homePct = round1(winPool * (homeStrength / strengthSum));
        double awayPct = round1(winPool * (awayStrength / strengthSum));
        drawPct = round1(100.0 - homePct - awayPct);

        double confidence = round1(Math.max(homePct, Math.max(drawPct, awayPct)));
        return new RankingProbabilities(homePct, drawPct, awayPct, confidence);
    }

    private String resolveWinnerName(
            Equipo local, Equipo visitante, double homePct, double drawPct, double awayPct) {
        if (drawPct >= homePct && drawPct >= awayPct) {
            return "Empate";
        }
        if (awayPct > homePct) {
            return visitante.getNombre();
        }
        return local.getNombre();
    }

    private String buildSpanishJustification(
            Partido partido,
            Equipo local,
            Equipo visitante,
            RankingProbabilities ranking,
            String predictedWinner) {
        String fase = partido.getFase() != null && !partido.getFase().isBlank()
                ? partido.getFase()
                : "fase de grupos";
        String favoritoLine;
        if ("Empate".equals(predictedWinner)) {
            favoritoLine = "El escenario más probable es un empate, con equipos muy parejos según el ranking.";
        } else if (predictedWinner.equals(local.getNombre())) {
            favoritoLine = local.getNombre()
                    + " parte como favorito combinando ranking FIFA y ventaja de jugar como local.";
        } else {
            favoritoLine = visitante.getNombre()
                    + " parte como favorito pese a jugar como visitante, por su mejor posición en el ranking FIFA.";
        }

        return """
                Análisis predictivo: %s vs %s (%s).

                Posiciones FIFA: %s (#%d) frente a %s (#%d).

                Distribución estimada: victoria local %.1f%%, empate %.1f%%, victoria visitante %.1f%%.

                %s Confianza del modelo: %.1f%%.
                """
                .formatted(
                        local.getNombre(),
                        visitante.getNombre(),
                        fase,
                        local.getNombre(),
                        local.getRankingFifa(),
                        visitante.getNombre(),
                        visitante.getRankingFifa(),
                        ranking.homePct(),
                        ranking.drawPct(),
                        ranking.awayPct(),
                        favoritoLine,
                        ranking.confidence())
                .trim();
    }

    private boolean isUnreliableLlmOutput(LlmPredictionResponse llm) {
        if (llm == null) {
            return true;
        }
        if ("v1-basic-score".equals(llm.modelVersion())) {
            return true;
        }
        double home = llm.homeWinProbability() != null ? llm.homeWinProbability() : 0;
        double away = llm.awayWinProbability() != null ? llm.awayWinProbability() : 0;
        return Math.abs(home - away) < 5.0;
    }

    private static double normalizeLlmPercent(Double value, double defaultValue) {
        if (value == null || value.isNaN()) {
            return defaultValue;
        }
        return value;
    }

    private static double parseConfidence(String nivelConfianza) {
        if (nivelConfianza == null) {
            return 0;
        }
        String digits = nivelConfianza.replaceAll("[^0-9.]", "");
        if (digits.isBlank()) {
            return 0;
        }
        return Double.parseDouble(digits);
    }

    private static String truncateExplanation(String text) {
        if (text == null) {
            return null;
        }
        final int maxLen = 8000;
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }

    private static String formatConfidence(Double accuracy) {
        double value = accuracy != null && !accuracy.isNaN() ? accuracy : 0;
        String label = Math.round(value) + "%";
        return label.length() <= 30 ? label : label.substring(0, 30);
    }

    /** Convierte porcentaje (0-100) o fracción (0-1) a valor 0-1 para la BD. */
    private static double toFraction(Double value, double defaultFraction) {
        if (value == null || value.isNaN()) {
            return defaultFraction;
        }
        return value > 1 ? value / 100.0 : value;
    }
}
