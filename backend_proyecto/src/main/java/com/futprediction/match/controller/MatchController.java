package com.futprediction.match.controller;

import com.futprediction.auth.entity.Usuario;
import com.futprediction.match.dto.CreateMatchEventRequestDTO;
import com.futprediction.match.dto.MatchCreateRequestDTO;
import com.futprediction.match.dto.MatchResponseDTO;
import com.futprediction.match.dto.MatchViewDTO;
import com.futprediction.match.dto.OfficializeMatchRequestDTO;
import com.futprediction.match.dto.UpdateLiveClockRequestDTO;
import com.futprediction.match.dto.UpdateLiveScoreRequestDTO;
import com.futprediction.match.service.MatchService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public ResponseEntity<List<MatchViewDTO>> listMatches() {
        return ResponseEntity.ok(matchService.listMatchesForUi());
    }

    @GetMapping("/live")
    public ResponseEntity<List<MatchViewDTO>> listLiveMatches() {
        return ResponseEntity.ok(matchService.listLiveMatchesForUi());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchViewDTO> getMatch(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.getMatchViewById(id));
    }

    @PostMapping
    public ResponseEntity<MatchResponseDTO> scheduleMatch(
            @Valid @RequestBody MatchCreateRequestDTO request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matchService.scheduleMatch(request, actor));
    }

    @PutMapping("/{id}/result")
    public ResponseEntity<MatchViewDTO> officialize(
            @PathVariable Long id,
            @Valid @RequestBody OfficializeMatchRequestDTO request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(matchService.officializeResult(id, request, actor));
    }

    @PostMapping("/{id}/start-live")
    public ResponseEntity<MatchViewDTO> startLive(
            @PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(matchService.startMatchLive(id, actor));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<MatchViewDTO> suspend(
            @PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(matchService.suspendMatch(id, actor));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<MatchViewDTO> resume(
            @PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(matchService.resumeMatch(id, actor));
    }

    @PutMapping("/{id}/live-clock")
    public ResponseEntity<MatchViewDTO> updateLiveClock(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLiveClockRequestDTO request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(matchService.updateLiveClock(id, request, actor));
    }

    @PostMapping("/{id}/halftime")
    public ResponseEntity<MatchViewDTO> enterHalftime(
            @PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(matchService.enterHalftime(id, actor));
    }

    @PostMapping("/{id}/resume-half")
    public ResponseEntity<MatchViewDTO> resumeAfterHalftime(
            @PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(matchService.resumeAfterHalftime(id, actor));
    }

    @PutMapping("/{id}/live-score")
    public ResponseEntity<MatchViewDTO> updateLiveScore(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLiveScoreRequestDTO request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(matchService.updateLiveScore(id, request, actor));
    }

    @PostMapping("/{id}/events")
    public ResponseEntity<MatchViewDTO> addEvent(
            @PathVariable Long id,
            @Valid @RequestBody CreateMatchEventRequestDTO request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(matchService.addMatchEvent(id, request, actor));
    }

    @DeleteMapping("/{id}/events/{eventId}")
    public ResponseEntity<MatchViewDTO> deleteEvent(
            @PathVariable Long id,
            @PathVariable Long eventId,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(matchService.deleteMatchEvent(id, eventId, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MatchViewDTO> updateMatch(
            @PathVariable Long id, @Valid @RequestBody MatchCreateRequestDTO request) {
        return ResponseEntity.ok(matchService.updateMatch(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }
}
