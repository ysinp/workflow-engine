package com.example.riskscore.service.rule;

import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@code src/main/resources/rules/risk-rules.drl} directly, independent of
 * {@link com.example.riskscore.service.RiskScoringServiceImpl}.
 */
class RiskRulesDrlTest {

    private final KieContainer kieContainer = KieServices.Factory.get().getKieClasspathContainer();

    @Test
    void highRiskJurisdiction_firesForEachListedNationality() {
        for (String nationality : new String[] {"IR", "KP", "SY", "CU", "SD"}) {
            List<RuleOutcome> outcomes = fire(new RiskFact(nationality, "ID-CLEAN-001"));
            assertThat(outcomes).as("nationality " + nationality)
                    .extracting(RuleOutcome::points).containsExactly(100);
            assertThat(outcomes.get(0).reason())
                    .isEqualTo("Nationality " + nationality + " is on the high-risk jurisdiction list");
        }
    }

    @Test
    void highRiskJurisdiction_doesNotFireForOtherNationality() {
        assertThat(fire(new RiskFact("US", "ID-CLEAN-001"))).isEmpty();
    }

    @Test
    void highRiskJurisdiction_doesNotFireForNullNationality() {
        assertThat(fire(new RiskFact(null, "ID-CLEAN-001"))).isEmpty();
    }

    @Test
    void testMarker_highFiresWithPositivePoints() {
        List<RuleOutcome> outcomes = fire(new RiskFact("US", "ID-RISK-HIGH-001"));
        assertThat(outcomes).extracting(RuleOutcome::points).containsExactly(100);
        assertThat(outcomes.get(0).reason()).isEqualTo("ID number contains the RISK-HIGH test marker");
    }

    @Test
    void testMarker_highIsCaseInsensitive() {
        assertThat(fire(new RiskFact("US", "id-risk-high-001")))
                .extracting(RuleOutcome::points).containsExactly(100);
    }

    @Test
    void testMarker_lowFiresWithNegativePoints() {
        List<RuleOutcome> outcomes = fire(new RiskFact("US", "ID-RISK-LOW-001"));
        assertThat(outcomes).extracting(RuleOutcome::points).containsExactly(-100);
        assertThat(outcomes.get(0).reason()).isEqualTo("ID number contains the RISK-LOW test marker");
    }

    @Test
    void testMarker_doesNotFireWithoutAMarker() {
        assertThat(fire(new RiskFact("US", "ID-CLEAN-001"))).isEmpty();
    }

    @Test
    void testMarker_doesNotFireWithNullIdNumber() {
        assertThat(fire(new RiskFact("US", null))).isEmpty();
    }

    @Test
    void bothRulesFire_whenJurisdictionAndMarkerBothMatch() {
        List<RuleOutcome> outcomes = fire(new RiskFact("IR", "ID-RISK-LOW-001"));
        assertThat(outcomes).extracting(RuleOutcome::points).containsExactlyInAnyOrder(100, -100);
    }

    private List<RuleOutcome> fire(RiskFact fact) {
        KieSession kieSession = kieContainer.newKieSession("risk-rules-ksession");
        try {
            kieSession.insert(fact);
            kieSession.fireAllRules();
            return kieSession.getObjects(obj -> obj instanceof RuleOutcome).stream()
                    .map(RuleOutcome.class::cast)
                    .toList();
        } finally {
            kieSession.dispose();
        }
    }
}
