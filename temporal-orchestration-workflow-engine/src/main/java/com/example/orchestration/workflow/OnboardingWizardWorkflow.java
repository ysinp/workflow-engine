package com.example.orchestration.workflow;

import com.example.orchestration.activity.RiskAssessment;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Durably owns the entire sign-up journey, Product Selection through Security (and, on a HIGH
 * risk verdict, a compliance reviewer's decision) - started the moment a customer lands on the
 * product-selection screen. identification-service, kyc-service, create-account and risk-score
 * are all invoked as Activities from inside this workflow; none of them are ever called directly
 * by the browser.
 *
 * <p>Every step is <em>also</em> its own genuine, parked Temporal Activity (see
 * {@code StepGateActivities}): the workflow schedules one per step, in strict sequence, and each
 * stays open - visible in Temporal UI's Pending Activities - until a human completes it. The
 * {@code @UpdateMethod}s below are what the browser actually calls; each one does the real I/O for
 * its step (via the usual Activities) and, once it succeeds, the REST layer completes that step's
 * gate activity, letting this workflow advance to the next one. Because the workflow only ever
 * schedules the next gate in order, a step whose gate has already completed can never be
 * reopened - {@link #submitPersonalContact} (for example) rejects a call once the workflow has
 * moved past "personal", rather than silently redoing it.
 */
@WorkflowInterface
public interface OnboardingWizardWorkflow {

    @WorkflowMethod
    AccountCreationResult execute();

    /** Product-selection screen's "Get started" button - the very first step of the journey. */
    @UpdateMethod
    ProductSelectionResult submitProductSelection(ProductSelectionData data);

    /** Wizard step 1 (Personal + Contact). Synchronous request/response against the running execution. */
    @UpdateMethod
    WizardStepResult submitPersonalContact(PersonalContactData data);

    /** Wizard step 2 (Identification). */
    @UpdateMethod
    WizardStepResult submitIdentification(IdentificationData data);

    /** Wizard step 3 (Financial + KYC verification). */
    @UpdateMethod
    FinancialVerificationOutcome submitFinancial(FinancialData data);

    /**
     * Wizard step 4 (Security): stages the application and evaluates risk. Never blocks on a
     * reviewer - a HIGH risk verdict returns {@code awaitingReview=true} immediately and the
     * frontend polls the execution's status for the eventual outcome.
     */
    @UpdateMethod
    AccountCreationStepResult submitSecurity(SecurityData data);

    /** True only while a HIGH-risk execution is paused waiting for the reviewer's gate activity to complete. */
    @QueryMethod
    boolean isAwaitingReview();

    /** The risk assessment a reviewer is being asked to act on; null before/after the wait. */
    @QueryMethod
    RiskAssessment getPendingRiskAssessment();

    /** The step name (activity ID) this execution is currently parked on - see SignUp.tsx STEP_SLUGS. */
    @QueryMethod
    String getCurrentStep();
}
