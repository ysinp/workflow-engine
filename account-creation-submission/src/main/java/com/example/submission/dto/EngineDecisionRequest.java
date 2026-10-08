package com.example.submission.dto;

/** Request body sent to {@code POST {base}/api/orchestration/executions/{workflowId}/decision}. */
public record EngineDecisionRequest(String decision, String reviewerId, String notes) {

    public static EngineDecisionRequest from(SubmitReviewDecisionRequest request) {
        return new EngineDecisionRequest(request.decision(), request.reviewerId(), request.notes());
    }
}
