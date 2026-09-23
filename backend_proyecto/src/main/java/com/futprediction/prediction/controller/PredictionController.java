package com.futprediction.prediction.controller;

import com.futprediction.auth.entity.Usuario;
import com.futprediction.prediction.dto.BracketSimulationDTO;
import com.futprediction.prediction.dto.GenerateAndSaveResponseDTO;
import com.futprediction.prediction.dto.PredictionHistoryDTO;
import com.futprediction.prediction.dto.PredictionResultDTO;
import com.futprediction.prediction.dto.PredictionStatsDTO;
import com.futprediction.prediction.dto.SavePredictionRequestDTO;
import com.futprediction.prediction.service.BracketSimulationService;
import com.futprediction.prediction.service.PredictionIntegrationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/predictions")
public class PredictionController {

    private final PredictionIntegrationService predictionService;
    private final BracketSimulationService bracketSimulationService;

    public PredictionController(
            PredictionIntegrationService predictionService,
            BracketSimulationService bracketSimulationService) {
        this.predictionService = predictionService;
        this.bracketSimulationService = bracketSimulationService;
    }

    @PostMapping("/bracket/simulate")
    public ResponseEntity<BracketSimulationDTO> simulateBracket() {
        return ResponseEntity.ok(bracketSimulationService.simulate());
    }

    @PostMapping("/matches/{matchId}/generate")
    public ResponseEntity<PredictionResultDTO> generate(
            @PathVariable Long matchId, @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        PredictionResultDTO result = predictionService.generateForMatch(matchId);
        return ResponseEntity.ok(result);
    }

    /** Genera y persiste en historial del usuario en un solo paso. */
    @PostMapping("/matches/{matchId}/generate-and-save")
    public ResponseEntity<GenerateAndSaveResponseDTO> generateAndSave(
            @PathVariable Long matchId, @AuthenticationPrincipal Usuario usuario) {
        PredictionResultDTO result = predictionService.generateForMatch(matchId);
        SavePredictionRequestDTO saveRequest = predictionService.buildSaveRequestFromResult(result);
        PredictionHistoryDTO history = predictionService.savePrediction(saveRequest, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GenerateAndSaveResponseDTO(result, history));
    }

    @PostMapping
    public ResponseEntity<PredictionHistoryDTO> save(
            @Valid @RequestBody SavePredictionRequestDTO request, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(predictionService.savePrediction(request, usuario));
    }

    @GetMapping("/history")
    public ResponseEntity<List<PredictionHistoryDTO>> history(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(predictionService.getHistory(usuario));
    }

    @GetMapping("/stats")
    public ResponseEntity<PredictionStatsDTO> stats(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(predictionService.getStats(usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        predictionService.deletePrediction(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
