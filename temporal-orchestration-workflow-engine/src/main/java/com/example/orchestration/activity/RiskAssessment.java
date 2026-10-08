package com.example.orchestration.activity;

import java.util.List;

/** Result of a call to the risk-score service. */
public record RiskAssessment(String riskLevel, int score, List<String> reasons) {

    public static final String LOW = "LOW";
    public static final String HIGH = "HIGH";
}
