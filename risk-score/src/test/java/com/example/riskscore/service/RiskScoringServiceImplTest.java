package com.example.riskscore.service;

import com.example.riskscore.dto.RiskEvaluationRequest;
import com.example.riskscore.dto.RiskEvaluationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;

import static org.assertj.core.api.Assertions.assertThat;

class RiskScoringServiceImplTest {

    private RiskScoringService riskScoringService;

    @BeforeEach
    void setUp() {
        KieContainer kieContainer = KieServices.Factory.get().getKieClasspathContainer();
        riskScoringService = new RiskScoringServiceImpl(kieContainer);
    }

    @Test
    void evaluate_withHighRiskNationalityAlone_returnsHigh() {
        RiskEvaluationResponse response = riskScoringService.evaluate(requestWith("IR", "ID-CLEAN-001"));

        assertThat(response.score()).isEqualTo(100);
        assertThat(response.riskLevel()).isEqualTo("HIGH");
        assertThat(response.reasons()).containsExactly("Nationality IR is on the high-risk jurisdiction list");
    }

    @Test
    void evaluate_withRiskHighMarkerAlone_returnsHigh() {
        RiskEvaluationResponse response = riskScoringService.evaluate(requestWith("US", "ID-RISK-HIGH-001"));

        assertThat(response.score()).isEqualTo(100);
        assertThat(response.riskLevel()).isEqualTo("HIGH");
        assertThat(response.reasons()).containsExactly("ID number contains the RISK-HIGH test marker");
    }

    @Test
    void evaluate_withCleanProfile_returnsLow() {
        RiskEvaluationResponse response = riskScoringService.evaluate(requestWith("US", "ID-CLEAN-001"));

        assertThat(response.score()).isEqualTo(0);
        assertThat(response.riskLevel()).isEqualTo("LOW");
        assertThat(response.reasons()).isEmpty();
    }

    /**
     * Non-obvious interaction called out explicitly in the spec: a high-risk
     * nationality (+100) combined with the RISK-LOW demo marker (-100) sums to 0
     * *before* clamping, and clamping (max(0, sum)) only ever raises a negative sum
     * to 0 - it never suppresses a positive one. So this profile still nets a score
     * of 0, which is < 50, i.e. LOW: the RISK-LOW marker reliably overrides a
     * high-risk nationality for demo purposes.
     */
    @Test
    void evaluate_withRiskLowMarkerOverridingHighRiskNationality_returnsLow() {
        RiskEvaluationResponse response = riskScoringService.evaluate(requestWith("IR", "ID-RISK-LOW-001"));

        assertThat(response.score()).isEqualTo(0);
        assertThat(response.riskLevel()).isEqualTo("LOW");
        assertThat(response.reasons()).containsExactlyInAnyOrder(
                "Nationality IR is on the high-risk jurisdiction list",
                "ID number contains the RISK-LOW test marker");
    }

    private RiskEvaluationRequest requestWith(String nationality, String idNumber) {
        return new RiskEvaluationRequest(
                "Jane Doe", "jane@example.com", "2000-01-01", nationality,
                "PASSPORT", idNumber, "Engineer", "UNDER_25K", "EMPLOYMENT", null);
    }
}
