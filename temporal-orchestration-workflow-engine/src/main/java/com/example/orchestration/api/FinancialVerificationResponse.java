package com.example.orchestration.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * {@code nextActivity} and {@code activityId} are both absent when {@code verified} is false -
 * the wizard stays on this step. See {@link StartWizardResponse} for what {@code activityId} is.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FinancialVerificationResponse(String workflowId, Long identificationId, boolean verified,
                                             List<String> reasons, String nextActivity, String activityId) {
}
