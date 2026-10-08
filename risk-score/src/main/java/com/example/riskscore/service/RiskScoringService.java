package com.example.riskscore.service;

import com.example.riskscore.dto.RiskEvaluationRequest;
import com.example.riskscore.dto.RiskEvaluationResponse;

public interface RiskScoringService {

    RiskEvaluationResponse evaluate(RiskEvaluationRequest request);
}
