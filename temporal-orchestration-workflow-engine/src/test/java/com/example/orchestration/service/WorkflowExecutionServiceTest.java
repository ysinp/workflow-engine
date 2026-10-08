package com.example.orchestration.service;

import com.example.orchestration.activity.AccountActivities;
import com.example.orchestration.activity.OnboardingActivities;
import com.example.orchestration.activity.RiskAssessment;
import com.example.orchestration.activity.StepGateActivitiesImpl;
import com.example.orchestration.workflow.AccountCreationResult;
import com.example.orchestration.workflow.FinancialData;
import com.example.orchestration.workflow.FinancialVerificationOutcome;
import com.example.orchestration.workflow.IdentificationData;
import com.example.orchestration.workflow.OnboardingWizardWorkflow;
import com.example.orchestration.workflow.OnboardingWizardWorkflowImpl;
import com.example.orchestration.workflow.PersonalContactData;
import com.example.orchestration.workflow.ProductSelectionData;
import com.example.orchestration.workflow.SecurityData;
import com.example.orchestration.workflow.WizardStepResult;
import io.temporal.client.ActivityCompletionClient;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Exercises the describe/listAwaitingReview status mapping used by the execution-status
 * endpoints, against Temporal's in-memory test server. Reviewer-decision completion itself is
 * {@link OnboardingWizardService}'s responsibility now (see OnboardingWizardWorkflowImplTest for
 * that), not this service's.
 */
public class WorkflowExecutionServiceTest {

    private static final PersonalContactData PERSONAL = new PersonalContactData(
            "Ada Lovelace", "1990-01-01", "US", "", "ada@example.com", "", "1 Main St", "",
            "Metropolis", "", "", "US");
    private static final IdentificationData IDENTIFICATION = new IdentificationData("PASSPORT", "A1234567");
    private static final FinancialData FINANCIAL = new FinancialData("Engineer", "UNDER_25K", "EMPLOYMENT");

    // useTimeskipping(false): see the equivalent @Rule comment in OnboardingWizardWorkflowImplTest
    // - the AWAITING_REVIEW test below polls a query before the workflow closes, which races the
    // default time-skipping clock into a runaway skip to the execution timeout.
    @Rule
    public TestWorkflowRule testWorkflowRule = TestWorkflowRule.newBuilder()
            .setWorkflowTypes(OnboardingWizardWorkflowImpl.class)
            .setDoNotStart(true)
            .setUseTimeskipping(false)
            .build();

    private PendingActivityTokenRegistry registry;
    private ActivityCompletionClient completionClient;
    private OnboardingActivities onboardingActivities;
    private AccountActivities accountActivities;
    private WorkflowExecutionService service;

    @Before
    public void setUp() {
        registry = new PendingActivityTokenRegistry();
        onboardingActivities = mock(OnboardingActivities.class, withSettings().withoutAnnotations());
        accountActivities = mock(AccountActivities.class, withSettings().withoutAnnotations());
        when(onboardingActivities.savePersonalContact(any(), any())).thenReturn(new WizardStepResult(1L, "identification"));
        when(onboardingActivities.updateIdentification(any(), any())).thenReturn(new WizardStepResult(1L, "financial"));
        when(onboardingActivities.verifyFinancial(any(), any()))
                .thenReturn(new FinancialVerificationOutcome(1L, true, List.of(), "security"));
        when(accountActivities.checkSubmission(anyString())).thenReturn(false);
        when(accountActivities.stageApplication(any())).thenReturn("app-1");
        testWorkflowRule.getWorker().registerActivitiesImplementations(
                onboardingActivities, accountActivities, new StepGateActivitiesImpl(registry));
        testWorkflowRule.getTestEnvironment().start();
        completionClient = testWorkflowRule.getWorkflowClient().newActivityCompletionClient();
        service = new WorkflowExecutionService(testWorkflowRule.getWorkflowClient());
    }

    @Test
    public void completedExecutionIncludesResult() {
        when(accountActivities.evaluateRisk(any())).thenReturn(new RiskAssessment(RiskAssessment.LOW, 0, List.of()));
        String workflowId = runThroughSecurity();

        WorkflowExecutionService.ExecutionState state = service.describe(workflowId);

        assertEquals(WorkflowExecutionService.STATUS_COMPLETED, state.status());
        assertEquals(AccountCreationResult.APPROVED, state.result().outcome());
        assertEquals(RiskAssessment.LOW, state.result().riskLevel());
        assertNull(state.errorMessage());
    }

    @Test
    public void highRiskExecutionReportsAwaitingReviewWithPendingRiskContext() {
        RiskAssessment highRisk = new RiskAssessment(RiskAssessment.HIGH, 100, List.of("reason"));
        when(accountActivities.evaluateRisk(any())).thenReturn(highRisk);
        String workflowId = "onboarding-test-" + testWorkflowRule.getUniquePostfix();
        OnboardingWizardWorkflow stub = newStub(workflowId);
        WorkflowClient.start(stub::execute);
        runStepsUpToSecurity(stub, workflowId);
        stub.submitSecurity(new SecurityData("Sup3rSecret!"));
        completeGate(stub, workflowId, "security");
        awaitTrue(stub::isAwaitingReview);

        WorkflowExecutionService.ExecutionState state = service.describe(workflowId);

        assertEquals(WorkflowExecutionService.STATUS_AWAITING_REVIEW, state.status());
        assertEquals(highRisk, state.pendingRiskAssessment());
        assertNull(state.result());
    }

    // listAwaitingReview() isn't covered here: it relies on WorkflowClient.listExecutions, backed
    // by Temporal's visibility/List API, which Temporal's in-memory TestWorkflowEnvironment does
    // not implement (StatusRuntimeException: UNIMPLEMENTED). It's exercised instead via the live
    // Docker stack against a real Temporal server.

    /**
     * A risk-score outage now surfaces as a failed submitSecurity Update call (same as any other
     * step's activity failure) rather than a terminal workflow failure, since that logic moved
     * from the standalone workflow's main method into an Update handler - the execution itself
     * stays RUNNING so the frontend's existing "Retry" affordance can simply re-submit the step.
     */
    @Test
    public void securityStepFailureLeavesTheExecutionRunningForRetry() {
        when(accountActivities.evaluateRisk(any())).thenThrow(new IllegalStateException("risk-score is down"));
        String workflowId = "onboarding-test-" + testWorkflowRule.getUniquePostfix();
        OnboardingWizardWorkflow stub = newStub(workflowId);
        WorkflowClient.start(stub::execute);
        runStepsUpToSecurity(stub, workflowId);

        assertThrows(Exception.class, () -> stub.submitSecurity(new SecurityData("Sup3rSecret!")));
        verify(accountActivities, times(3)).evaluateRisk(any()); // RetryOptions.maximumAttempts

        WorkflowExecutionService.ExecutionState state = service.describe(workflowId);
        assertEquals(WorkflowExecutionService.STATUS_RUNNING, state.status());
    }

    @Test
    public void unknownWorkflowIdThrowsNotFound() {
        assertThrows(ExecutionNotFoundException.class, () -> service.describe("onboarding-does-not-exist"));
    }

    private OnboardingWizardWorkflow newStub(String workflowId) {
        return testWorkflowRule.getWorkflowClient().newWorkflowStub(
                OnboardingWizardWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(testWorkflowRule.getTaskQueue())
                        .setWorkflowId(workflowId)
                        .build());
    }

    /** Runs product through financial, completing each step's gate - stops just before Security. */
    private void runStepsUpToSecurity(OnboardingWizardWorkflow stub, String workflowId) {
        stub.submitProductSelection(new ProductSelectionData("Elite Pay Personal Checking"));
        completeGate(stub, workflowId, "product");
        stub.submitPersonalContact(PERSONAL);
        completeGate(stub, workflowId, "personal");
        stub.submitIdentification(IDENTIFICATION);
        completeGate(stub, workflowId, "identification");
        stub.submitFinancial(FINANCIAL);
        completeGate(stub, workflowId, "financial");
    }

    /** Runs every step (including Security) and waits for the execution to close (APPROVED or FAILED). */
    private String runThroughSecurity() {
        String workflowId = "onboarding-test-" + testWorkflowRule.getUniquePostfix();
        OnboardingWizardWorkflow stub = newStub(workflowId);
        WorkflowClient.start(stub::execute);
        runStepsUpToSecurity(stub, workflowId);
        stub.submitSecurity(new SecurityData("Sup3rSecret!"));
        completeGate(stub, workflowId, "security");
        try {
            testWorkflowRule.getWorkflowClient().newUntypedWorkflowStub(workflowId)
                    .getResult(AccountCreationResult.class);
        } catch (RuntimeException ignored) {
            // failure case: we only need the execution to be closed
        }
        return workflowId;
    }

    /**
     * Completing an activity only durably records the event server-side - it does not wait for
     * the workflow's own thread to actually process it and advance. Poll getCurrentStep() until
     * it does, so the next call this test makes sees the workflow in its new state (exactly the
     * gap a human filling out the next step's form naturally closes in production).
     */
    private void completeGate(OnboardingWizardWorkflow stub, String workflowId, String step) {
        PendingActivityTokenRegistry.PendingGate gate = registry.take(workflowId, step);
        assertNotNull("gate token for '" + step + "' should have been registered", gate);
        completionClient.complete(gate.taskToken(), null);
        awaitTrue(() -> !step.equals(stub.getCurrentStep()));
    }

    /** Safe only with useTimeskipping(false) - see the @Rule comment above. */
    private static void awaitTrue(java.util.function.BooleanSupplier condition) {
        for (int i = 0; i < 200; i++) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        throw new AssertionError("Condition did not become true within timeout");
    }
}
