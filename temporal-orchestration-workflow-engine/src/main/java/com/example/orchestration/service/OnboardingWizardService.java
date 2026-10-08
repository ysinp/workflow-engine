package com.example.orchestration.service;

import com.example.orchestration.api.AccountCreationStepResponse;
import com.example.orchestration.api.FinancialVerificationResponse;
import com.example.orchestration.api.ProductSelectionResponse;
import com.example.orchestration.api.StartWizardResponse;
import com.example.orchestration.api.WizardStepResponse;
import com.example.orchestration.config.TemporalConstants;
import com.example.orchestration.workflow.AccountCreationStepResult;
import com.example.orchestration.workflow.FinancialData;
import com.example.orchestration.workflow.FinancialVerificationOutcome;
import com.example.orchestration.workflow.IdentificationData;
import com.example.orchestration.workflow.OnboardingWizardWorkflow;
import com.example.orchestration.workflow.PersonalContactData;
import com.example.orchestration.workflow.ProductSelectionData;
import com.example.orchestration.workflow.ReviewDecision;
import com.example.orchestration.workflow.SecurityData;
import com.example.orchestration.workflow.WizardStepResult;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.ActivityCompletionClient;
import io.temporal.client.ActivityCompletionException;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowOptions;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Thin client-side facade over {@link WorkflowClient} for the onboarding wizard workflow - started
 * the moment a customer lands on the product-selection screen. Every step's "what's next"
 * decision is decided by the matching {@code @UpdateMethod} on {@link OnboardingWizardWorkflow},
 * which does the real I/O for that step; this service's other job is completing that step's
 * parked gate activity afterward (see {@code StepGateActivities}), which is what actually lets
 * the workflow advance - the Update call alone does not. It also peeks the *next* gate's real
 * Temporal activity ID (never consuming it) to include in the response as {@code activityId}.
 */
@Service
public class OnboardingWizardService {

    private static final String WORKFLOW_ID_PREFIX = "onboarding-";

    private final WorkflowClient workflowClient;
    private final ActivityCompletionClient activityCompletionClient;
    private final PendingActivityTokenRegistry tokenRegistry;

    public OnboardingWizardService(WorkflowClient workflowClient, PendingActivityTokenRegistry tokenRegistry) {
        this.workflowClient = workflowClient;
        this.activityCompletionClient = workflowClient.newActivityCompletionClient();
        this.tokenRegistry = tokenRegistry;
    }

    public StartWizardResponse start() {
        String workflowId = WORKFLOW_ID_PREFIX + UUID.randomUUID();
        OnboardingWizardWorkflow stub = workflowClient.newWorkflowStub(
                OnboardingWizardWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TemporalConstants.TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .build());
        // Asynchronous start: returns as soon as Temporal has durably recorded the execution; the
        // workflow method itself just sequences through each step's gate activity (see
        // OnboardingWizardWorkflowImpl), so there is nothing to wait for here.
        WorkflowExecution execution = WorkflowClient.start(stub::execute);
        String workflowIdActual = execution.getWorkflowId();
        return new StartWizardResponse(workflowIdActual, "product", peekActivityId(workflowIdActual, "product"));
    }

    /** @throws ExecutionNotFoundException if Temporal has no execution with this workflow ID */
    public ProductSelectionResponse submitProductSelection(String workflowId, ProductSelectionData data) {
        call(workflowId, () -> attach(workflowId).submitProductSelection(data));
        completeGate(workflowId, "product", null);
        return new ProductSelectionResponse(workflowId, "personal", peekActivityId(workflowId, "personal"));
    }

    /** @throws ExecutionNotFoundException if Temporal has no execution with this workflow ID */
    public WizardStepResponse submitPersonalContact(String workflowId, PersonalContactData data) {
        WizardStepResult result = call(workflowId, () -> attach(workflowId).submitPersonalContact(data));
        completeGate(workflowId, "personal", null);
        return new WizardStepResponse(workflowId, result.identificationId(), "identification",
                peekActivityId(workflowId, "identification"));
    }

    /** @throws ExecutionNotFoundException if Temporal has no execution with this workflow ID */
    public WizardStepResponse submitIdentification(String workflowId, IdentificationData data) {
        WizardStepResult result = call(workflowId, () -> attach(workflowId).submitIdentification(data));
        completeGate(workflowId, "identification", null);
        return new WizardStepResponse(workflowId, result.identificationId(), "financial",
                peekActivityId(workflowId, "financial"));
    }

    /** @throws ExecutionNotFoundException if Temporal has no execution with this workflow ID */
    public FinancialVerificationResponse submitFinancial(String workflowId, FinancialData data) {
        FinancialVerificationOutcome outcome = call(workflowId, () -> attach(workflowId).submitFinancial(data));
        if (!outcome.verified()) {
            // stays parked on "financial" - the gate is not completed, so the user can retry
            return new FinancialVerificationResponse(workflowId, outcome.identificationId(), false,
                    outcome.reasons(), null, null);
        }
        completeGate(workflowId, "financial", null);
        return new FinancialVerificationResponse(workflowId, outcome.identificationId(), true, outcome.reasons(),
                "security", peekActivityId(workflowId, "security"));
    }

    /**
     * Stages the application and evaluates risk; never blocks on a reviewer (see
     * {@link OnboardingWizardWorkflow#submitSecurity}).
     *
     * @throws ExecutionNotFoundException if Temporal has no execution with this workflow ID
     */
    public AccountCreationStepResponse submitSecurity(String workflowId, SecurityData data) {
        AccountCreationStepResult result = call(workflowId, () -> attach(workflowId).submitSecurity(data));
        completeGate(workflowId, "security", null);
        String nextActivity = result.awaitingReview() ? "review" : null;
        String activityId = result.awaitingReview() ? peekActivityId(workflowId, "review") : null;
        return new AccountCreationStepResponse(workflowId, result.riskLevel(), result.score(), result.reasons(),
                result.awaitingReview(), result.result(), nextActivity, activityId);
    }

    /**
     * A compliance reviewer's decision on a HIGH-risk application paused in AWAITING_REVIEW -
     * completes the "review" gate activity directly (there is no Update for this step; the
     * decision itself *is* the gate's completion value, consumed by {@code execute()}).
     *
     * @throws ExecutionNotFoundException if this execution has no "review" gate currently pending
     */
    public void submitReviewDecision(String workflowId, ReviewDecision decision) {
        completeGate(workflowId, "review", decision);
    }

    private <R> R call(String workflowId, java.util.function.Supplier<R> updateCall) {
        try {
            return updateCall.get();
        } catch (WorkflowNotFoundException e) {
            throw new ExecutionNotFoundException(workflowId);
        }
    }

    /** @throws ExecutionNotFoundException if this step's gate activity isn't currently pending */
    private void completeGate(String workflowId, String step, Object result) {
        PendingActivityTokenRegistry.PendingGate gate = tokenRegistry.take(workflowId, step);
        if (gate == null) {
            throw new ExecutionNotFoundException(workflowId);
        }
        try {
            activityCompletionClient.complete(gate.taskToken(), result);
        } catch (ActivityCompletionException e) {
            throw new ExecutionNotFoundException(workflowId);
        }
    }

    /**
     * Temporal's own ID for the gate activity now pending on {@code step} - peeked (never
     * consumed) so the step that actually completes it later still can. Null if that gate hasn't
     * been scheduled/registered within the wait (see PendingActivityTokenRegistry); never fails
     * the request over this, since it's purely informational.
     */
    private String peekActivityId(String workflowId, String step) {
        PendingActivityTokenRegistry.PendingGate gate = tokenRegistry.peek(workflowId, step);
        return gate == null ? null : gate.temporalActivityId();
    }

    // newWorkflowStub(Class, workflowId) only builds a local proxy - it never contacts the
    // server, so WorkflowNotFoundException can only ever surface from the actual Update call
    // each method above makes, never from this method itself.
    private OnboardingWizardWorkflow attach(String workflowId) {
        return workflowClient.newWorkflowStub(OnboardingWizardWorkflow.class, workflowId);
    }
}
