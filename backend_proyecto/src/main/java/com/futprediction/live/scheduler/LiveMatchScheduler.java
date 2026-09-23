package com.futprediction.live.scheduler;

import com.futprediction.match.service.MatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LiveMatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(LiveMatchScheduler.class);

    private final MatchService matchService;

    public LiveMatchScheduler(MatchService matchService) {
        this.matchService = matchService;
    }

    /** Cada 5 s: partidos programados cuya hora (Colombia) ya llegó pasan a EN_CURSO. */
    @Scheduled(fixedRate = 5_000, initialDelay = 5_000)
    public void activateScheduledMatches() {
        int activated = matchService.activateDueMatches();
        if (activated > 0) {
            log.info("Partidos activados en vivo: {}", activated);
        }
    }
}
