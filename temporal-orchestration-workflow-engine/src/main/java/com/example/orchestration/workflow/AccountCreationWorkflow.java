package com.example.orchestration.workflow;

import com.example.orchestration.activity.RiskAssessment;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * The account-creation business process, as designed in
 * {@code src/main/resources/workflows/create-account-workflow.json}.
 *
 * <p>HIGH-risk applications pause here for a human compliance decision (see
 * {@link AccountCreationWorkflowImpl}) - the workflow blocks on {@code Workflow.await} until
 * {@link #submitReviewDecision} delivers a signal, exactly the same durable-wait pattern used
 * throughout this project. LOW-risk applications are unaffected and still auto-approve.
 */
@WorkflowInterface
public interface AccountCreationWorkflow {

    @WorkflowMethod
    AccountCreationResult execute(AccountCreationInput input);

    /** Delivered by a compliance reviewer via the REST decision endpoint. */
    @SignalMethod
    void submitReviewDecision(ReviewDecision decision);

    /** True only while a HIGH-risk execution is paused waiting for {@link #submitReviewDecision}. */
    @QueryMethod
    boolean isAwaitingReview();

    /** The risk assessment a reviewer is being asked to act on; null before/after the wait. */
    @QueryMethod
    RiskAssessment getPendingRiskAssessment();
}
