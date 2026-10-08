package com.example.orchestration.workflow;

import com.example.orchestration.activity.AccountActivities;
import com.example.orchestration.activity.RiskAssessment;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Unit tests for the workflow orchestration logic, run against Temporal's in-memory test
 * server (no external Temporal, no create-account/risk-score services).
 */
public class AccountCreationWorkflowImplTest {

    private static final AccountCreationInput INPUT = new AccountCreationInput(
            "app-1", "Ada Lovelace", "ada@example.com", "1990-01-01", "US", null,
            null, null, null, null, null, null, "PASSPORT", "A1234567", null, null, null);

    // doNotStart: the mocked activities must be registered before the worker starts.
    // useTimeskipping(false): the HITL tests below pause the workflow on Workflow.await with no
    // timer to bound it, then poll a query in a loop before signaling. With the default
    // time-skipping clock, the environment auto-advances virtual time to the next scheduled
    // event whenever nothing currently holds its "time lock" - and a bare Thread.sleep-based
    // poll loop doesn't hold that lock between iterations, so the clock races ahead to the
    // (very large default) execution timeout in the gaps. Disabling time-skipping makes the
    // test clock track real time 1:1, so ordinary polling is safe again.
    @Rule
    public TestWorkflowRule testWorkflowRule = TestWorkflowRule.newBuilder()
            .setWorkflowTypes(AccountCreationWorkflowImpl.class)
            .setDoNotStart(true)
            .setUseTimeskipping(false)
            .build();

    @Test
    public void approvesAndNotifiesWhenRiskIsLow() {
        AccountActivities activities = registerMockActivities();
        RiskAssessment lowRisk = new RiskAssessment(RiskAssessment.LOW, 0, List.of());
        when(activities.checkSubmission(INPUT.email())).thenReturn(false);
        when(activities.evaluateRisk(INPUT)).thenReturn(lowRisk);

        AccountCreationResult result = newWorkflowStub().execute(INPUT);

        assertEquals(AccountCreationResult.APPROVED, result.outcome());
        assertEquals(RiskAssessment.LOW, result.riskLevel());
        assertNull("LOW risk auto-approves, no reviewer involved", result.reviewedBy());
        verify(activities).checkSubmission(INPUT.email());
        verify(activities).evaluateRisk(INPUT);
        verify(activities).activateAccount(INPUT.applicationId());
        verify(activities).notifyApproved(INPUT.email(), INPUT.fullName());
        verify(activities, never()).rejectApplication(anyString());
        verify(activities, never()).notifyRejected(anyString(), anyString(), anyList());
    }

    @Test
    public void highRiskPausesForReviewThenApprovesWhenReviewerApproves() {
        AccountActivities activities = registerMockActivities();
        RiskAssessment highRisk = new RiskAssessment(RiskAssessment.HIGH, 100,
                List.of("Nationality US is on the high-risk jurisdiction list"));
        when(activities.checkSubmission(INPUT.email())).thenReturn(false);
        when(activities.evaluateRisk(INPUT)).thenReturn(highRisk);

        AccountCreationWorkflow stub = newWorkflowStub();
        WorkflowClient.start(stub::execute, INPUT);

        // A plain query answers with whatever state exists right now - it does NOT force the
        // workflow to first finish processing already-scheduled work, so calling it immediately
        // after an async start races the worker thread. Polling is safe here only because
        // useTimeskipping(false) is set above; with the default time-skipping clock a bare
        // Thread.sleep-based poll loop lets the environment's auto time-skip run away to the
        // execution timeout in the gaps, since nothing holds its "time lock" between iterations.
        awaitTrue(stub::isAwaitingReview);
        assertEquals(highRisk, stub.getPendingRiskAssessment());

        stub.submitReviewDecision(new ReviewDecision(ReviewDecision.APPROVE, "reviewer-1", "looks fine on manual check"));

        AccountCreationResult result = getResult(stub);
        assertEquals(AccountCreationResult.APPROVED, result.outcome());
        assertEquals("reviewer-1", result.reviewedBy());
        verify(activities).activateAccount(INPUT.applicationId());
        verify(activities).notifyApproved(INPUT.email(), INPUT.fullName());
        verify(activities, never()).rejectApplication(anyString());
        verify(activities, never()).notifyRejected(anyString(), anyString(), anyList());
    }

    @Test
    public void highRiskPausesForReviewThenRejectsWhenReviewerRejects() {
        AccountActivities activities = registerMockActivities();
        RiskAssessment highRisk = new RiskAssessment(RiskAssessment.HIGH, 100, List.of("reason"));
        when(activities.checkSubmission(INPUT.email())).thenReturn(false);
        when(activities.evaluateRisk(INPUT)).thenReturn(highRisk);

        AccountCreationWorkflow stub = newWorkflowStub();
        WorkflowClient.start(stub::execute, INPUT);
        awaitTrue(stub::isAwaitingReview);

        stub.submitReviewDecision(new ReviewDecision(ReviewDecision.REJECT, "reviewer-2", null));

        AccountCreationResult result = getResult(stub);
        assertEquals(AccountCreationResult.REJECTED, result.outcome());
        assertEquals("reviewer-2", result.reviewedBy());
        verify(activities).rejectApplication(INPUT.applicationId());
        verify(activities).notifyRejected(INPUT.email(), INPUT.fullName(), highRisk.reasons());
        verify(activities, never()).activateAccount(anyString());
        verify(activities, never()).notifyApproved(anyString(), anyString());
    }

    private AccountActivities registerMockActivities() {
        // withSettings().withoutAnnotations() mirrors Temporal's official Java samples for mocked
        // activities (the mock should not carry copied annotations into Temporal's reflection).
        AccountActivities activities = mock(AccountActivities.class, withSettings().withoutAnnotations());
        testWorkflowRule.getWorker().registerActivitiesImplementations(activities);
        testWorkflowRule.getTestEnvironment().start();
        return activities;
    }

    private AccountCreationWorkflow newWorkflowStub() {
        return testWorkflowRule.getWorkflowClient().newWorkflowStub(
                AccountCreationWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    }

    private static AccountCreationResult getResult(AccountCreationWorkflow stub) {
        return WorkflowStub.fromTyped(stub).getResult(AccountCreationResult.class);
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
