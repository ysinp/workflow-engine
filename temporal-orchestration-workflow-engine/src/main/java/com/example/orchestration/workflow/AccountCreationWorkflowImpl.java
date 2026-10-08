package com.example.orchestration.workflow;

import com.example.orchestration.activity.AccountActivities;
import com.example.orchestration.activity.RiskAssessment;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * Pure orchestration: decides which activity runs next. No HTTP, no I/O, no non-deterministic
 * calls here - everything with side effects lives in the activity implementation.
 *
 * <p>Maps 1:1 to the nodes of {@code workflows/create-account-workflow.json} (see README):
 * Verify Account Submission -> Calculate Risk Score -> Validate Risk Score decision -> notify.
 *
 * <p>This workflow now GATES account creation: {@code input.applicationId()} identifies a staged
 * application in create-account that has no real Account row (and no sign-in capability) yet.
 * Reaching {@link AccountCreationResult#APPROVED} activates it (creates the Account row);
 * reaching {@link AccountCreationResult#REJECTED} leaves it rejected forever - no Account row is
 * ever created for it.
 *
 * <p><b>Human-in-the-loop:</b> a LOW risk verdict still auto-approves instantly, matching the
 * diagram exactly. A HIGH risk verdict no longer auto-rejects - instead the workflow pauses on
 * {@code Workflow.await} until a compliance reviewer delivers a decision via
 * {@link #submitReviewDecision}. This is a durable wait: the worker process can restart, the
 * workflow holds no thread or resource while paused, and it resumes deterministically from
 * Temporal's event history the instant the signal arrives - the same pattern the very first KYC
 * demo in this project used for its human-review step.
 */
public class AccountCreationWorkflowImpl implements AccountCreationWorkflow {

    // Replay-safe logger (suppresses duplicate log lines during history replay).
    private static final Logger log = Workflow.getLogger(AccountCreationWorkflowImpl.class);

    private final AccountActivities activities = Workflow.newActivityStub(
            AccountActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setBackoffCoefficient(2.0)
                            .setMaximumAttempts(3)
                            .build())
                    .build());

    // Signal-handler state. Only ever written by submitReviewDecision, read by the workflow
    // method and the two query methods - all on the single workflow thread, so no
    // synchronization is needed (Temporal serializes signal handlers against workflow code).
    private ReviewDecision reviewDecision;
    private RiskAssessment pendingRiskAssessment;

    @Override
    public AccountCreationResult execute(AccountCreationInput input) {
        // action-check-duplicate ("Verify Account Submission") - logged for audit only; the
        // current workflow design (matching the diagram) does not branch on this result.
        boolean alreadySubmitted = activities.checkSubmission(input.email());
        log.info("Submission check for {} completed (alreadySubmitted={})", input.email(), alreadySubmitted);

        // action "Calculate Risk Score"
        RiskAssessment risk = activities.evaluateRisk(input);

        // decision "Validate Risk Score"
        if (RiskAssessment.HIGH.equals(risk.riskLevel())) {
            return awaitReviewAndNotify(input, risk);
        }

        log.info("Risk level LOW for {}; auto-approving", input.email());
        activities.activateAccount(input.applicationId());
        activities.notifyApproved(input.email(), input.fullName());
        return new AccountCreationResult(AccountCreationResult.APPROVED, risk.riskLevel(), risk.score(),
                risk.reasons(), null);
    }

    private AccountCreationResult awaitReviewAndNotify(AccountCreationInput input, RiskAssessment risk) {
        log.info("Risk level HIGH for {}; pausing for a compliance reviewer's decision", input.email());
        pendingRiskAssessment = risk;
        Workflow.await(() -> reviewDecision != null);
        pendingRiskAssessment = null;

        if (ReviewDecision.isApprove(reviewDecision)) {
            log.info("Reviewer {} approved the HIGH-risk application for {}", reviewDecision.reviewerId(), input.email());
            activities.activateAccount(input.applicationId());
            activities.notifyApproved(input.email(), input.fullName());
            return new AccountCreationResult(AccountCreationResult.APPROVED, risk.riskLevel(), risk.score(),
                    risk.reasons(), reviewDecision.reviewerId());
        }

        log.info("Reviewer {} rejected the HIGH-risk application for {}", reviewDecision.reviewerId(), input.email());
        activities.rejectApplication(input.applicationId());
        activities.notifyRejected(input.email(), input.fullName(), risk.reasons());
        return new AccountCreationResult(AccountCreationResult.REJECTED, risk.riskLevel(), risk.score(),
                risk.reasons(), reviewDecision.reviewerId());
    }

    @Override
    public void submitReviewDecision(ReviewDecision decision) {
        if (this.reviewDecision != null) {
            log.warn("Ignoring duplicate review decision signal; first decision already recorded");
            return;
        }
        if (!ReviewDecision.APPROVE.equals(decision.decision()) && !ReviewDecision.REJECT.equals(decision.decision())) {
            log.warn("Ignoring review decision signal with unrecognized decision '{}'", decision.decision());
            return;
        }
        this.reviewDecision = decision;
    }

    @Override
    public boolean isAwaitingReview() {
        return pendingRiskAssessment != null;
    }

    @Override
    public RiskAssessment getPendingRiskAssessment() {
        return pendingRiskAssessment;
    }
}
