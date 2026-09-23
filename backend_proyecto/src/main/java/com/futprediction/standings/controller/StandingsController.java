package com.futprediction.standings.controller;

import com.futprediction.standings.dto.StandingDTO;
import com.futprediction.standings.service.StandingsService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/standings")
public class StandingsController {

    private final StandingsService standingsService;

    public StandingsController(StandingsService standingsService) {
        this.standingsService = standingsService;
    }

    @GetMapping
    public ResponseEntity<Map<String, List<StandingDTO>>> allStandings() {
        return ResponseEntity.ok(standingsService.getAllStandings());
    }

    @GetMapping("/groups/{group}")
    public ResponseEntity<List<StandingDTO>> groupStandings(@PathVariable String group) {
        return ResponseEntity.ok(standingsService.getStandingsForGroup(group));
    }
}
