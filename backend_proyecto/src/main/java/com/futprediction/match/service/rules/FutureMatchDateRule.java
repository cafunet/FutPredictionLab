package com.futprediction.match.service.rules;

import com.futprediction.match.dto.MatchCreateRequestDTO;
import com.futprediction.match.support.MatchColombiaTime;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Valida la fecha del partido en hora Colombia, coherente con el selector datetime-local del admin.
 */
@Component
public class FutureMatchDateRule implements MatchSchedulingRule {

    @Override
    public void validate(MatchCreateRequestDTO request) {
        LocalDateTime fechaHora = request.fechaHora();
        if (fechaHora == null) {
            return;
        }
        ZonedDateTime scheduled = fechaHora.atZone(MatchColombiaTime.ZONE);
        if (!scheduled.isAfter(MatchColombiaTime.nowZoned())) {
            throw new IllegalArgumentException(
                    "La fecha y hora del partido deben ser posteriores al momento actual (hora Colombia).");
        }
    }
}
