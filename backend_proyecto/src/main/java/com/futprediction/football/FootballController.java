package com.futprediction.football;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/football")
public class FootballController {
    private final ApiSportsClient client;

    public FootballController(ApiSportsClient client) {
        this.client = client;
    }

    @GetMapping("/teams")
    public JsonNode teams(@RequestParam String search) {
        return client.teams(search);
    }

    @GetMapping("/players/squads")
    public JsonNode squad(@RequestParam long team) {
        return client.squad(team);
    }

    @GetMapping("/coachs")
    public JsonNode coaches(@RequestParam long team) {
        return client.coaches(team);
    }
}
