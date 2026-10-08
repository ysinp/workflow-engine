package com.example.orchestration.api;

import com.example.orchestration.workflow.AccountCreationResult;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * {@code result} is present only when the Security step auto-approved synchronously (LOW risk);
 * a HIGH risk verdict returns with {@code awaitingReview=true}, {@code nextActivity="review"} and
 * no result - the frontend polls the execution's status for the reviewer's eventual outcome.
 * {@code activityId} is Temporal's own ID for that "review" gate - see {@link StartWizardResponse}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountCreationStepResponse(String workflowId, String riskLevel, int score, List<String> reasons,
                                          boolean awaitingReview, AccountCreationResult result, String nextActivity,
                                          String activityId) {
}
