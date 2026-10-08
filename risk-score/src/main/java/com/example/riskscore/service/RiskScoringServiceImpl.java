package com.example.riskscore.service;

import com.example.riskscore.dto.RiskEvaluationRequest;
import com.example.riskscore.dto.RiskEvaluationResponse;
import com.example.riskscore.service.rule.RiskFact;
import com.example.riskscore.service.rule.RuleOutcome;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Runs the Drools rule base compiled from {@code src/main/resources/rules/risk-rules.drl} (see
 * {@link com.example.riskscore.config.DroolsConfig}). Each rule that fires inserts a
 * {@link RuleOutcome} fact; after firing, every {@code RuleOutcome} left in working memory is
 * collected, summed, and clamped to a minimum of 0 before the LOW/HIGH decision.
 */
@Service
public class RiskScoringServiceImpl implements RiskScoringService {

    private static final int HIGH_RISK_THRESHOLD = 50;
    private static final String KSESSION_NAME = "risk-rules-ksession";

    private final KieContainer kieContainer;

    public RiskScoringServiceImpl(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    @Override
    public RiskEvaluationResponse evaluate(RiskEvaluationRequest request) {
        // A fresh session per request: this service is stateless, and a session must never be
        // reused across requests - leftover facts from a prior evaluation would silently corrupt
        // the next one.
        KieSession kieSession = kieContainer.newKieSession(KSESSION_NAME);
        try {
            kieSession.insert(new RiskFact(request.nationality(), request.idNumber()));
            kieSession.fireAllRules();

            List<RuleOutcome> outcomes = kieSession.getObjects(fact -> fact instanceof RuleOutcome).stream()
                    .map(RuleOutcome.class::cast)
                    .toList();

            int total = outcomes.stream().mapToInt(RuleOutcome::points).sum();
            List<String> reasons = outcomes.stream().map(RuleOutcome::reason).toList();

            int score = Math.max(0, total);
            String riskLevel = score >= HIGH_RISK_THRESHOLD ? "HIGH" : "LOW";
            return new RiskEvaluationResponse(riskLevel, score, reasons);
        } finally {
            kieSession.dispose();
        }
    }
}
