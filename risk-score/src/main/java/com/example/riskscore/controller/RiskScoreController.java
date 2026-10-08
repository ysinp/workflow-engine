package com.example.riskscore.controller;

import com.example.riskscore.dto.RiskEvaluationRequest;
import com.example.riskscore.dto.RiskEvaluationResponse;
import com.example.riskscore.service.RiskScoringService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Plain REST surface for stateless risk scoring. No business logic lives here -
 * everything is delegated to the RiskScoringService interface.
 */
@RestController
@RequestMapping("/api/risk-score")
public class RiskScoreController {

    private final RiskScoringService riskScoringService;

    public RiskScoreController(RiskScoringService riskScoringService) {
        this.riskScoringService = riskScoringService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<RiskEvaluationResponse> evaluate(@Valid @RequestBody RiskEvaluationRequest request) {
        return ResponseEntity.ok(riskScoringService.evaluate(request));
    }
}
