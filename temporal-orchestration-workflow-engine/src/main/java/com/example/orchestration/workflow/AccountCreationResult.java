package com.example.orchestration.workflow;

import java.util.List;

/**
 * Workflow result: a risk-assessment verdict and notification outcome. This workflow no longer
 * creates an account (that happens synchronously elsewhere, outside Temporal) - it purely
 * evaluates risk for an already-submitted KYC application and notifies the applicant.
 *
 * @param outcome    {@link #APPROVED} or {@link #REJECTED}
 * @param riskLevel  "LOW" or "HIGH", as returned by the risk-score service
 * @param score      the numeric risk score that produced this verdict
 * @param reasons    human-readable reasons the risk-score service gave for the score
 * @param reviewedBy null for an auto-approved LOW-risk outcome; the reviewer's id when a HIGH-risk
 *                   outcome was decided by a human via {@link AccountCreationWorkflow#submitReviewDecision}
 */
public record AccountCreationResult(String outcome, String riskLevel, int score, List<String> reasons,
                                     String reviewedBy) {

    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
}
