package com.example.orchestration.api;

import com.example.orchestration.service.OnboardingWizardService;
import com.example.orchestration.service.ReviewerAuthGuard;
import com.example.orchestration.service.WorkflowExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Inspects and signals onboarding wizard executions by workflow ID. Starting one is owned by
 * {@link OnboardingController} instead - this controller only ever attaches to an execution
 * already running.
 */
@RestController
@RequestMapping("/api/orchestration")
public class WorkflowExecutionController {

    private final WorkflowExecutionService executions;
    private final OnboardingWizardService wizardService;
    private final ReviewerAuthGuard reviewerAuthGuard;

    public WorkflowExecutionController(WorkflowExecutionService executions, OnboardingWizardService wizardService,
                                        ReviewerAuthGuard reviewerAuthGuard) {
        this.executions = executions;
        this.wizardService = wizardService;
        this.reviewerAuthGuard = reviewerAuthGuard;
    }

    /** Public: backs both the applicant's own "awaiting approval" status page and reviewer lookup. */
    @GetMapping("/executions/{workflowId}")
    public ExecutionStatusResponse getExecution(@PathVariable String workflowId) {
        return ExecutionStatusResponse.from(executions.describe(workflowId));
    }

    /** Backs the Review Console's pending-cases queue. REVIEWER-only. */
    @GetMapping("/executions/awaiting-review")
    public List<ExecutionStatusResponse> listAwaitingReview(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        reviewerAuthGuard.requireReviewer(authorization);
        return executions.listAwaitingReview().stream().map(ExecutionStatusResponse::from).toList();
    }

    /** A compliance reviewer's decision on a HIGH-risk application paused in AWAITING_REVIEW. REVIEWER-only. */
    @PostMapping("/executions/{workflowId}/decision")
    public ResponseEntity<Void> submitReviewDecision(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String workflowId,
            @Valid @RequestBody SubmitReviewDecisionRequest request) {
        reviewerAuthGuard.requireReviewer(authorization);
        wizardService.submitReviewDecision(workflowId, request.toReviewDecision());
        return ResponseEntity.accepted().build();
    }
}
