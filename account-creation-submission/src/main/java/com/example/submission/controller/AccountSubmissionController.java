package com.example.submission.controller;

import com.example.submission.dto.SubmissionAcceptedResponse;
import com.example.submission.dto.SubmissionStatusResponse;
import com.example.submission.dto.SubmitAccountRequest;
import com.example.submission.dto.SubmitReviewDecisionRequest;
import com.example.submission.service.ReviewerAuthGuard;
import com.example.submission.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
 * The single base URL the frontend calls to submit a new account-creation request and poll
 * its status. No business logic lives here - everything is delegated to the
 * {@link SubmissionService} interface.
 */
@RestController
@RequestMapping("/api/account-submissions")
public class AccountSubmissionController {

    private final SubmissionService submissionService;
    private final ReviewerAuthGuard reviewerAuthGuard;

    public AccountSubmissionController(SubmissionService submissionService, ReviewerAuthGuard reviewerAuthGuard) {
        this.submissionService = submissionService;
        this.reviewerAuthGuard = reviewerAuthGuard;
    }

    @PostMapping
    public ResponseEntity<SubmissionAcceptedResponse> submit(@Valid @RequestBody SubmitAccountRequest request) {
        SubmissionAcceptedResponse response = submissionService.submit(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{submissionId}")
    public ResponseEntity<SubmissionStatusResponse> getStatus(@PathVariable String submissionId) {
        return ResponseEntity.ok(submissionService.getStatus(submissionId));
    }

    /** A compliance reviewer's decision on a submission currently AWAITING_REVIEW. REVIEWER-only. */
    @PostMapping("/{submissionId}/decision")
    public ResponseEntity<Void> submitReviewDecision(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String submissionId,
            @Valid @RequestBody SubmitReviewDecisionRequest request) {
        reviewerAuthGuard.requireReviewer(authorization);
        submissionService.submitReviewDecision(submissionId, request);
        return ResponseEntity.accepted().build();
    }

    /** Backs the Review Console's pending-cases queue. REVIEWER-only. */
    @GetMapping("/pending-review")
    public List<SubmissionStatusResponse> listAwaitingReview(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        reviewerAuthGuard.requireReviewer(authorization);
        return submissionService.listAwaitingReview();
    }
}
