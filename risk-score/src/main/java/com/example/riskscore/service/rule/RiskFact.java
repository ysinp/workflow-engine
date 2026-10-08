package com.example.riskscore.service.rule;

/**
 * The input fact Drools pattern-matches against in {@code src/main/resources/rules/risk-rules.drl}.
 * Deliberately a plain JavaBean, not the {@code RiskEvaluationRequest} record directly - DRL field
 * constraints resolve properties via getters, and keeping this fact minimal (only the fields a
 * rule actually reads) keeps the rule base's dependency on the request DTO narrow.
 */
public class RiskFact {

    private final String nationality;
    private final String idNumber;

    public RiskFact(String nationality, String idNumber) {
        this.nationality = nationality;
        this.idNumber = idNumber;
    }

    public String getNationality() {
        return nationality;
    }

    public String getIdNumber() {
        return idNumber;
    }
}
