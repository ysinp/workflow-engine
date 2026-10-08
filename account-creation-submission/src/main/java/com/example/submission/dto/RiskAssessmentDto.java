package com.example.submission.dto;

import java.util.List;

/** Mirrors the engine's RiskAssessment shape: passed through, not interpreted here. */
public record RiskAssessmentDto(String riskLevel, int score, List<String> reasons) {
}
