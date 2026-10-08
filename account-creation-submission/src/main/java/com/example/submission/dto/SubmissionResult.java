package com.example.submission.dto;

import java.util.List;

/**
 * Outcome payload of a completed risk-assessment submission, passed through from the
 * orchestration engine.
 */
public record SubmissionResult(
        String outcome,
        String riskLevel,
        int score,
        List<String> reasons
) {
}
