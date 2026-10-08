package com.example.orchestration.api;

/** See {@link StartWizardResponse} for what {@code activityId} is and why it can be null. */
public record WizardStepResponse(String workflowId, Long identificationId, String nextActivity, String activityId) {
}
