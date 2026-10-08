package com.example.submission.service;

import com.example.submission.dto.SubmissionAcceptedResponse;
import com.example.submission.dto.SubmissionStatusResponse;
import com.example.submission.dto.SubmitAccountRequest;
import com.example.submission.dto.SubmitReviewDecisionRequest;

import java.util.List;

public interface SubmissionService {

    SubmissionAcceptedResponse submit(SubmitAccountRequest request);

    SubmissionStatusResponse getStatus(String submissionId);

    /** A compliance reviewer's decision on a submission currently AWAITING_REVIEW. */
    void submitReviewDecision(String submissionId, SubmitReviewDecisionRequest request);

    /** Every submission currently paused for a compliance decision - backs the review queue. */
    List<SubmissionStatusResponse> listAwaitingReview();
}
