package com.example.riskscore.dto;

import java.util.List;

/**
 * Response body for POST /api/risk-score/evaluate.
 * {@code riskLevel} is binary ("LOW" or "HIGH") to match the calling workflow's
 * two-branch approve/reject decision - there is deliberately no MEDIUM tier.
 */
public record RiskEvaluationResponse(String riskLevel, int score, List<String> reasons) {
}
