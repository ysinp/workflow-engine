package com.example.riskscore.service.rule;

/**
 * Fact a Drools rule inserts when it fires - see
 * {@code src/main/resources/rules/risk-rules.drl}. A rule that doesn't match simply inserts
 * nothing; there is no "fired with zero points" instance.
 */
public record RuleOutcome(int points, String reason) {
}
