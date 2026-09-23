package com.futprediction.match.support;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Hora de referencia para programación y partidos en vivo (Colombia). */
public final class MatchColombiaTime {

    public static final ZoneId ZONE = ZoneId.of("America/Bogota");

    private MatchColombiaTime() {}

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static ZonedDateTime nowZoned() {
        return ZonedDateTime.now(ZONE);
    }

    /** Serializa hora Colombia como ISO-8601 con offset (-05:00). */
    public static String toIsoOffset(LocalDateTime local) {
        if (local == null) {
            return null;
        }
        return local.atZone(ZONE).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
