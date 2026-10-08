package com.example.orchestration.api;

/**
 * {@code activityId} is Temporal's own auto-assigned ID for the "product" gate activity this
 * execution is now parked on (see Temporal UI's Pending Activities) - purely informational,
 * distinct from {@code nextActivity}, our own step-name identifier. Null only if that gate hadn't
 * been scheduled yet by the time this responded (see PendingActivityTokenRegistry).
 */
public record StartWizardResponse(String workflowId, String nextActivity, String activityId) {
}
