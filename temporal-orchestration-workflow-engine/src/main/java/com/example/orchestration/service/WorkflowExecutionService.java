package com.example.orchestration.service;

import com.example.orchestration.activity.RiskAssessment;
import com.example.orchestration.workflow.AccountCreationResult;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowStub;
import io.temporal.failure.TemporalFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Thin client-side facade over {@link WorkflowClient}: inspects and signals onboarding wizard
 * executions. Starting an execution is owned by {@link OnboardingWizardService}; this service
 * only ever attaches to one already running, by its workflow ID.
 */
@Service
public class WorkflowExecutionService {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_AWAITING_REVIEW = "AWAITING_REVIEW";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_UNKNOWN = "UNKNOWN";

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionService.class);
    private static final String AWAITING_REVIEW_QUERY =
            "WorkflowType='OnboardingWizardWorkflow' AND ExecutionStatus='Running'";

    private final WorkflowClient workflowClient;

    public WorkflowExecutionService(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    /**
     * @throws ExecutionNotFoundException if Temporal has no execution with this workflow ID
     */
    public ExecutionState describe(String workflowId) {
        WorkflowStub stub = workflowClient.newUntypedWorkflowStub(workflowId);
        WorkflowExecutionStatus temporalStatus;
        try {
            temporalStatus = stub.describe().getStatus();
        } catch (WorkflowNotFoundException e) {
            throw new ExecutionNotFoundException(workflowId);
        }

        String status = simplify(temporalStatus);
        return switch (status) {
            case STATUS_RUNNING -> describeRunning(workflowId, stub);
            case STATUS_COMPLETED ->
                    // Already closed, so getResult returns immediately.
                    new ExecutionState(workflowId, status, stub.getResult(AccountCreationResult.class), null, null);
            case STATUS_FAILED -> new ExecutionState(workflowId, status, null, failureMessage(stub), null);
            default -> new ExecutionState(workflowId, status, null, null, null);
        };
    }

    /**
     * Every open execution currently paused for a compliance decision - backs the Review
     * Console's queue view so a reviewer doesn't need to already know a workflow ID. Uses
     * Temporal's visibility store (built-in WorkflowType/ExecutionStatus search attributes, no
     * custom ones needed) to list open executions, then queries each to filter down to the ones
     * actually awaiting review (RUNNING also covers "still in the wizard" or "still processing").
     */
    public List<ExecutionState> listAwaitingReview() {
        return workflowClient.listExecutions(AWAITING_REVIEW_QUERY)
                .map(metadata -> metadata.getExecution().getWorkflowId())
                .map(this::tryDescribeRunning)
                .filter(state -> state != null && STATUS_AWAITING_REVIEW.equals(state.status()))
                .toList();
    }

    /** Null on any failure (e.g. the execution closed between listing and querying it) - skip it. */
    private ExecutionState tryDescribeRunning(String workflowId) {
        try {
            return describeRunning(workflowId, workflowClient.newUntypedWorkflowStub(workflowId));
        } catch (RuntimeException e) {
            log.debug("Skipping {} while listing pending reviews: {}", workflowId, e.getMessage());
            return null;
        }
    }

    /** RUNNING covers "still in the wizard", "still processing" and "paused awaiting a reviewer". */
    private ExecutionState describeRunning(String workflowId, WorkflowStub stub) {
        boolean awaitingReview;
        try {
            awaitingReview = Boolean.TRUE.equals(stub.query("isAwaitingReview", Boolean.class));
        } catch (RuntimeException e) {
            // Query can transiently fail (e.g. worker not yet polling); fall back to plain RUNNING
            // rather than failing the whole status call over what's likely a momentary race.
            return new ExecutionState(workflowId, STATUS_RUNNING, null, null, null);
        }
        if (!awaitingReview) {
            return new ExecutionState(workflowId, STATUS_RUNNING, null, null, null);
        }
        RiskAssessment pending = stub.query("getPendingRiskAssessment", RiskAssessment.class);
        return new ExecutionState(workflowId, STATUS_AWAITING_REVIEW, null, null, pending);
    }

    private static String simplify(WorkflowExecutionStatus status) {
        return switch (status) {
            case WORKFLOW_EXECUTION_STATUS_RUNNING -> STATUS_RUNNING;
            case WORKFLOW_EXECUTION_STATUS_COMPLETED -> STATUS_COMPLETED;
            case WORKFLOW_EXECUTION_STATUS_FAILED -> STATUS_FAILED;
            default -> STATUS_UNKNOWN;
        };
    }

    private static String failureMessage(WorkflowStub stub) {
        try {
            stub.getResult(AccountCreationResult.class);
            return "Workflow reported FAILED but returned a result";
        } catch (WorkflowFailedException e) {
            return rootCauseMessage(e);
        }
    }

    /** Deepest meaningful message, e.g. the ApplicationFailure raised by the activity. */
    private static String rootCauseMessage(Throwable t) {
        String message = t.getMessage();
        for (Throwable c = t; c != null; c = c.getCause()) {
            String m = (c instanceof TemporalFailure tf) ? tf.getOriginalMessage() : c.getMessage();
            if (m != null && !m.isBlank()) {
                message = m;
            }
        }
        return message;
    }

    public record ExecutionState(String workflowId, String status, AccountCreationResult result,
                                 String errorMessage, RiskAssessment pendingRiskAssessment) {
    }
}
