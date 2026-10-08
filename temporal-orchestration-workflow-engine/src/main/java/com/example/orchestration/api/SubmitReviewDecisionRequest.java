package com.example.orchestration.api;

import com.example.orchestration.workflow.ReviewDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request body for {@code POST /api/orchestration/executions/{workflowId}/decision}. */
public record SubmitReviewDecisionRequest(
        @NotBlank
        @Pattern(regexp = "APPROVE|REJECT", message = "decision must be APPROVE or REJECT")
        String decision,

        @NotBlank
        String reviewerId,

        String notes) {

    public ReviewDecision toReviewDecision() {
        return new ReviewDecision(decision, reviewerId, notes);
    }
}
