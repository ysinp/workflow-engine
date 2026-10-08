package com.example.orchestration.workflow;

import com.example.orchestration.activity.AccountActivities;
import com.example.orchestration.activity.OnboardingActivities;
import com.example.orchestration.activity.RiskAssessment;
import com.example.orchestration.activity.StepGateActivitiesImpl;
import com.example.orchestration.service.PendingActivityTokenRegistry;
import io.temporal.client.ActivityCompletionClient;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.WorkflowUpdateException;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Exercises the merged onboarding wizard - Product Selection through Security, including the
 * HIGH-risk pause-for-reviewer branch and step-order enforcement - against Temporal's in-memory
 * test server. Each step here is completed the same two-part way {@code OnboardingWizardService}
 * does in production: the Update call does the real work, then completing that step's gate
 * activity (looked up by workflow ID + step name via {@link PendingActivityTokenRegistry}) is
 * what actually lets the workflow advance.
 */
public class OnboardingWizardWorkflowImplTest {

    private static final PersonalContactData PERSONAL = new PersonalContactData(
            "Ada Lovelace", "1990-01-01", "US", "", "ada@example.com", "", "1 Main St", "",
            "Metropolis", "", "", "US");
    private static final IdentificationData IDENTIFICATION = new IdentificationData("PASSPORT", "A1234567");
    private static final FinancialData FINANCIAL = new FinancialData("Engineer", "UNDER_25K", "EMPLOYMENT");

    // useTimeskipping(false): the HIGH-risk tests below pause the workflow with no timer to bound
    // it, then poll a query in a loop before signaling - see the equivalent comment this project
    // has used for every other human-in-the-loop workflow test.
    @Rule
    public TestWorkflowRule testWorkflowRule = TestWorkflowRule.newBuilder()
            .setWorkflowTypes(OnboardingWizardWorkflowImpl.class)
            .setDoNotStart(true)
            .setUseTimeskipping(false)
            .build();

    private PendingActivityTokenRegistry registry;
    private ActivityCompletionClient completionClient;
    private OnboardingActivities onboarding;
    private AccountActivities account;

    @Before
    public void setUp() {
        registry = new PendingActivityTokenRegistry();
        onboarding = mock(OnboardingActivities.class, withSettings().withoutAnnotations());
        account = mock(AccountActivities.class, withSettings().withoutAnnotations());
        testWorkflowRule.getWorker().registerActivitiesImplementations(
                onboarding, account, new StepGateActivitiesImpl(registry));
        testWorkflowRule.getTestEnvironment().start();
        completionClient = testWorkflowRule.getWorkflowClient().newActivityCompletionClient();
    }

    @Test
    public void happyPathAutoApprovesOnLowRisk() {
        when(onboarding.savePersonalContact(any(), any())).thenReturn(new WizardStepResult(1L, "identification"));
        when(onboarding.updateIdentification(any(), any())).thenReturn(new WizardStepResult(1L, "financial"));
        when(onboarding.verifyFinancial(any(), any()))
                .thenReturn(new FinancialVerificationOutcome(1L, true, List.of(), "security"));
        when(account.checkSubmission(anyString())).thenReturn(false);
        when(account.stageApplication(any())).thenReturn("app-1");
        when(account.evaluateRisk(any())).thenReturn(new RiskAssessment(RiskAssessment.LOW, 0, List.of()));

        OnboardingWizardWorkflow stub = newWorkflowStub();
        String workflowId = WorkflowClient.start(stub::execute).getWorkflowId();

        stub.submitProductSelection(new ProductSelectionData("Elite Pay Personal Checking"));
        completeGate(stub, workflowId, "product");

        assertEquals(new WizardStepResult(1L, "identification"), stub.submitPersonalContact(PERSONAL));
        completeGate(stub, workflowId, "personal");

        assertEquals(new WizardStepResult(1L, "financial"), stub.submitIdentification(IDENTIFICATION));
        completeGate(stub, workflowId, "identification");

        assertTrue(stub.submitFinancial(FINANCIAL).verified());
        completeGate(stub, workflowId, "financial");

        AccountCreationStepResult stepResult = stub.submitSecurity(new SecurityData("Sup3rSecret!"));
        completeGate(stub, workflowId, "security");
        assertFalse(stepResult.awaitingReview());
        assertEquals(AccountCreationResult.APPROVED, stepResult.result().outcome());
        assertNull("LOW risk auto-approves, no reviewer involved", stepResult.result().reviewedBy());

        AccountCreationResult result = WorkflowStub.fromTyped(stub).getResult(AccountCreationResult.class);
        assertEquals(AccountCreationResult.APPROVED, result.outcome());
        verify(account).activateAccount("app-1");
        verify(account).notifyApproved(PERSONAL.email(), PERSONAL.fullName());
        verify(account, never()).rejectApplication(anyString());
    }

    @Test
    public void highRiskPausesForReviewThenApprovesWhenReviewerApproves() {
        stubWorkThroughFinancial();
        RiskAssessment highRisk = new RiskAssessment(RiskAssessment.HIGH, 100,
                List.of("Nationality US is on the high-risk jurisdiction list"));
        when(account.evaluateRisk(any())).thenReturn(highRisk);

        OnboardingWizardWorkflow stub = newWorkflowStub();
        String workflowId = WorkflowClient.start(stub::execute).getWorkflowId();
        runUpToSecurity(stub, workflowId);

        AccountCreationStepResult stepResult = stub.submitSecurity(new SecurityData("Sup3rSecret!"));
        completeGate(stub, workflowId, "security");
        assertTrue(stepResult.awaitingReview());
        assertNull(stepResult.result());
        awaitTrue(stub::isAwaitingReview);
        assertEquals(highRisk, stub.getPendingRiskAssessment());

        completeGate(stub, workflowId, "review", new ReviewDecision(ReviewDecision.APPROVE, "reviewer-1", "looks fine"));

        AccountCreationResult result = WorkflowStub.fromTyped(stub).getResult(AccountCreationResult.class);
        assertEquals(AccountCreationResult.APPROVED, result.outcome());
        assertEquals("reviewer-1", result.reviewedBy());
        verify(account).activateAccount("app-1");
        verify(account).notifyApproved(PERSONAL.email(), PERSONAL.fullName());
        verify(account, never()).rejectApplication(anyString());
    }

    @Test
    public void highRiskPausesForReviewThenRejectsWhenReviewerRejects() {
        stubWorkThroughFinancial();
        RiskAssessment highRisk = new RiskAssessment(RiskAssessment.HIGH, 100, List.of("reason"));
        when(account.evaluateRisk(any())).thenReturn(highRisk);

        OnboardingWizardWorkflow stub = newWorkflowStub();
        String workflowId = WorkflowClient.start(stub::execute).getWorkflowId();
        runUpToSecurity(stub, workflowId);
        stub.submitSecurity(new SecurityData("Sup3rSecret!"));
        completeGate(stub, workflowId, "security");
        awaitTrue(stub::isAwaitingReview);

        completeGate(stub, workflowId, "review", new ReviewDecision(ReviewDecision.REJECT, "reviewer-2", null));

        AccountCreationResult result = WorkflowStub.fromTyped(stub).getResult(AccountCreationResult.class);
        assertEquals(AccountCreationResult.REJECTED, result.outcome());
        assertEquals("reviewer-2", result.reviewedBy());
        verify(account).rejectApplication("app-1");
        verify(account).notifyRejected(PERSONAL.email(), PERSONAL.fullName(), highRisk.reasons());
        verify(account, never()).activateAccount(anyString());
        verify(account, never()).notifyApproved(anyString(), anyString());
    }

    @Test
    public void resubmittingAnAlreadyCompletedStepIsRejected() {
        stubWorkThroughFinancial();
        when(account.evaluateRisk(any())).thenReturn(new RiskAssessment(RiskAssessment.LOW, 0, List.of()));

        OnboardingWizardWorkflow stub = newWorkflowStub();
        String workflowId = WorkflowClient.start(stub::execute).getWorkflowId();
        stub.submitProductSelection(new ProductSelectionData("Elite Pay Personal Checking"));
        completeGate(stub, workflowId, "product");
        stub.submitPersonalContact(PERSONAL);
        completeGate(stub, workflowId, "personal");
        stub.submitIdentification(IDENTIFICATION);
        completeGate(stub, workflowId, "identification");

        // The workflow has moved on to "financial" - resubmitting "personal" must be rejected,
        // not silently redone (see OnboardingWizardWorkflowImpl#requireCurrentStep).
        assertThrows(WorkflowUpdateException.class, () -> stub.submitPersonalContact(PERSONAL));
    }

    private void stubWorkThroughFinancial() {
        when(onboarding.savePersonalContact(any(), any())).thenReturn(new WizardStepResult(1L, "identification"));
        when(onboarding.updateIdentification(any(), any())).thenReturn(new WizardStepResult(1L, "financial"));
        when(onboarding.verifyFinancial(any(), any()))
                .thenReturn(new FinancialVerificationOutcome(1L, true, List.of(), "security"));
        when(account.checkSubmission(anyString())).thenReturn(false);
        when(account.stageApplication(any())).thenReturn("app-1");
    }

    private void runUpToSecurity(OnboardingWizardWorkflow stub, String workflowId) {
        stub.submitProductSelection(new ProductSelectionData("Elite Pay Personal Checking"));
        completeGate(stub, workflowId, "product");
        stub.submitPersonalContact(PERSONAL);
        completeGate(stub, workflowId, "personal");
        stub.submitIdentification(IDENTIFICATION);
        completeGate(stub, workflowId, "identification");
        stub.submitFinancial(FINANCIAL);
        completeGate(stub, workflowId, "financial");
    }

    private void completeGate(OnboardingWizardWorkflow stub, String workflowId, String step) {
        completeGate(stub, workflowId, step, null);
    }

    /**
     * Completing an activity only durably records the event server-side - it does not wait for
     * the workflow's own thread to actually process it and advance. Poll getCurrentStep() until
     * it does, so the next call this test makes sees the workflow in its new state (exactly the
     * gap a human filling out the next step's form naturally closes in production).
     */
    private void completeGate(OnboardingWizardWorkflow stub, String workflowId, String step, Object result) {
        PendingActivityTokenRegistry.PendingGate gate = registry.take(workflowId, step);
        assertNotNull("gate token for '" + step + "' should have been registered", gate);
        completionClient.complete(gate.taskToken(), result);
        awaitTrue(() -> !step.equals(stub.getCurrentStep()));
    }

    private OnboardingWizardWorkflow newWorkflowStub() {
        return testWorkflowRule.getWorkflowClient().newWorkflowStub(
                OnboardingWizardWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
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
