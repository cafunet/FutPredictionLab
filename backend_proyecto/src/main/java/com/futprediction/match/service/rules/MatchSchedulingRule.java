package com.futprediction.match.service.rules;

import com.futprediction.match.dto.MatchCreateRequestDTO;

public interface MatchSchedulingRule {
    void validate(MatchCreateRequestDTO request);
}
