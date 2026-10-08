package com.example.orchestration.workflow;

import com.example.orchestration.activity.AccountActivities;
import com.example.orchestration.activity.OnboardingActivities;
import com.example.orchestration.activity.RiskAssessment;
import com.example.orchestration.activity.StepGateActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * {@code execute()} is a pure sequencer: it schedules each step's gate activity in order and
 * blocks on it (see {@link StepGateActivities}), advancing {@link #currentStep} only once that
 * gate has actually been completed by a human via the REST layer. Every {@code @UpdateMethod}
 * below does the real work for its step (delegating I/O to {@link OnboardingActivities}/
 * {@link AccountActivities}) and guards itself with {@link #requireCurrentStep}, so a step can
 * never be resubmitted once the workflow has moved past it - both because the gate activity that
 * backed it is already complete, and because this guard rejects the attempt outright.
 */
public class OnboardingWizardWorkflowImpl implements OnboardingWizardWorkflow {

    /** {@link ApplicationFailure#getType()} used by {@link #requireCurrentStep} - see ApiExceptionHandler. */
    public static final String STEP_ORDER_VIOLATION_TYPE = "StepOrderViolation";

    private static final Logger log = Workflow.getLogger(OnboardingWizardWorkflowImpl.class);

    private static final ActivityOptions WORK_ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(3)
                    .build())
            .build();

    // Parked indefinitely awaiting a human; a single attempt only - retrying would mint a new
    // task token and orphan whatever the REST layer already registered for this step.
    private static final ActivityOptions GATE_ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofDays(30))
            .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
            .build();

    private final OnboardingActivities onboardingActivities =
            Workflow.newActivityStub(OnboardingActivities.class, WORK_ACTIVITY_OPTIONS);
    private final AccountActivities accountActivities =
            Workflow.newActivityStub(AccountActivities.class, WORK_ACTIVITY_OPTIONS);
    private final StepGateActivities gates =
            Workflow.newActivityStub(StepGateActivities.class, GATE_ACTIVITY_OPTIONS);

    private String currentStep = "product";
    private PersonalContactData personalContact;
    private IdentificationData identification;
    private FinancialData financial;
    private Long identificationId;
    private String applicationId;

    private boolean awaitingReview;
    private RiskAssessment pendingRiskAssessment;
    private RiskAssessment lastRiskAssessment;
    private AccountCreationResult finalResult;

    @Override
    public AccountCreationResult execute() {
        gates.awaitProductSelection();
        currentStep = "personal";

        gates.awaitPersonalContact();
        currentStep = "identification";

        gates.awaitIdentification();
        currentStep = "financial";

        gates.awaitFinancial();
        currentStep = "security";

        gates.awaitSecurity();

        if (awaitingReview) {
            currentStep = "review";
            log.info("Risk level HIGH; pausing for a compliance reviewer's decision");
            ReviewDecision decision = gates.awaitReviewDecision();
            finalResult = resolveReviewDecision(decision);
        }

        currentStep = "done";
        return finalResult;
    }

    private AccountCreationResult resolveReviewDecision(ReviewDecision decision) {
        awaitingReview = false;
        pendingRiskAssessment = null;
        if (ReviewDecision.isApprove(decision)) {
            log.info("Reviewer {} approved the HIGH-risk application for {}", decision.reviewerId(), personalContact.email());
            accountActivities.activateAccount(applicationId);
            accountActivities.notifyApproved(personalContact.email(), personalContact.fullName());
            return new AccountCreationResult(AccountCreationResult.APPROVED, lastRiskAssessment.riskLevel(),
                    lastRiskAssessment.score(), lastRiskAssessment.reasons(), decision.reviewerId());
        }
        log.info("Reviewer {} rejected the HIGH-risk application for {}", decision.reviewerId(), personalContact.email());
        accountActivities.rejectApplication(applicationId);
        accountActivities.notifyRejected(personalContact.email(), personalContact.fullName(), lastRiskAssessment.reasons());
        return new AccountCreationResult(AccountCreationResult.REJECTED, lastRiskAssessment.riskLevel(),
                lastRiskAssessment.score(), lastRiskAssessment.reasons(), decision.reviewerId());
    }

    @Override
    public ProductSelectionResult submitProductSelection(ProductSelectionData data) {
        requireCurrentStep("product");
        return new ProductSelectionResult(currentStep);
    }

    @Override
    public WizardStepResult submitPersonalContact(PersonalContactData data) {
        requireCurrentStep("personal");
        WizardStepResult result = onboardingActivities.savePersonalContact(identificationId, data);
        identificationId = result.identificationId();
        personalContact = data;
        return result;
    }

    @Override
    public WizardStepResult submitIdentification(IdentificationData data) {
        requireCurrentStep("identification");
        WizardStepResult result = onboardingActivities.updateIdentification(identificationId, data);
        identification = data;
        return result;
    }

    @Override
    public FinancialVerificationOutcome submitFinancial(FinancialData data) {
        requireCurrentStep("financial");
        FinancialVerificationOutcome outcome = onboardingActivities.verifyFinancial(identificationId, data);
        if (outcome.verified()) {
            financial = data;
        }
        return outcome;
    }

    @Override
    public AccountCreationStepResult submitSecurity(SecurityData data) {
        requireCurrentStep("security");

        // action-check-duplicate ("Verify Account Submission") - logged for audit only; does not branch.
        boolean alreadySubmitted = accountActivities.checkSubmission(personalContact.email());
        log.info("Submission check for {} completed (alreadySubmitted={})", personalContact.email(), alreadySubmitted);

        applicationId = accountActivities.stageApplication(new StageApplicationData(
                personalContact.fullName(), personalContact.email(), data.password(), personalContact.dateOfBirth(),
                personalContact.nationality(), personalContact.phone(), personalContact.addressLine1(),
                personalContact.addressLine2(), personalContact.city(), personalContact.state(),
                personalContact.postalCode(), personalContact.country(), identification.idType(),
                identification.idNumber(), financial.occupation(), financial.annualIncome(), financial.sourceOfFunds()));

        RiskAssessment risk = accountActivities.evaluateRisk(new AccountCreationInput(
                applicationId, personalContact.fullName(), personalContact.email(), personalContact.dateOfBirth(),
                personalContact.nationality(), personalContact.phone(), personalContact.addressLine1(),
                personalContact.addressLine2(), personalContact.city(), personalContact.state(),
                personalContact.postalCode(), personalContact.country(), identification.idType(),
                identification.idNumber(), financial.occupation(), financial.annualIncome(), financial.sourceOfFunds()));
        lastRiskAssessment = risk;

        if (RiskAssessment.HIGH.equals(risk.riskLevel())) {
            log.info("Risk level HIGH for {}; flagging for compliance review", personalContact.email());
            pendingRiskAssessment = risk;
            awaitingReview = true;
            return new AccountCreationStepResult(risk.riskLevel(), risk.score(), risk.reasons(), true, null);
        }

        log.info("Risk level LOW for {}; auto-approving", personalContact.email());
        accountActivities.activateAccount(applicationId);
        accountActivities.notifyApproved(personalContact.email(), personalContact.fullName());
        finalResult = new AccountCreationResult(AccountCreationResult.APPROVED, risk.riskLevel(), risk.score(),
                risk.reasons(), null);
        return new AccountCreationStepResult(risk.riskLevel(), risk.score(), risk.reasons(), false, finalResult);
    }

    @Override
    public boolean isAwaitingReview() {
        return awaitingReview;
    }

    @Override
    public RiskAssessment getPendingRiskAssessment() {
        return pendingRiskAssessment;
    }

    @Override
    public String getCurrentStep() {
        return currentStep;
    }

    /** Rejects an Update for a step this execution isn't currently parked on - see the class javadoc. */
    private void requireCurrentStep(String expected) {
        if (!expected.equals(currentStep)) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "This step is not currently active (workflow is on '" + currentStep + "')",
                    STEP_ORDER_VIOLATION_TYPE);
        }
    }
}
