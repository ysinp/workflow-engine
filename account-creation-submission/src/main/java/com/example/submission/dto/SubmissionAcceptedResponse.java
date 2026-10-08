package com.example.submission.dto;

/**
 * Response body for {@code POST /api/account-submissions} (HTTP 202).
 */
public record SubmissionAcceptedResponse(
        String submissionId,
        String status
) {
}
