package com.example.submission.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response body for {@code GET /api/account-submissions/{submissionId}}.
 * A passthrough of the orchestration engine's status response, with {@code workflowId}
 * renamed to {@code submissionId} for consistency with this service's own vocabulary.
 * {@code result}, {@code errorMessage} and {@code pendingRiskAssessment} are omitted from the
 * JSON when null. {@code pendingRiskAssessment} is only present when {@code status} is
 * {@code AWAITING_REVIEW}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubmissionStatusResponse(
        String submissionId,
        String status,
        SubmissionResult result,
        String errorMessage,
        RiskAssessmentDto pendingRiskAssessment
) {
}
