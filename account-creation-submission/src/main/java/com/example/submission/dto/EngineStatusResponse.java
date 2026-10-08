package com.example.submission.dto;

/**
 * Response body from {@code GET {base}/api/orchestration/executions/{workflowId}} on the
 * temporal-orchestration-workflow-engine service. {@code result}, {@code errorMessage} and
 * {@code pendingRiskAssessment} are absent from the JSON (and therefore null here) unless
 * applicable to the current status.
 */
public record EngineStatusResponse(
        String workflowId,
        String status,
        SubmissionResult result,
        String errorMessage,
        RiskAssessmentDto pendingRiskAssessment
) {
}
