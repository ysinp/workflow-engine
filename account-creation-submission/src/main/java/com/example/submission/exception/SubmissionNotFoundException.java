package com.example.submission.exception;

/**
 * Thrown when the orchestration engine reports (via HTTP 404) that it has no execution
 * matching the given submission/workflow id. Mapped to {@code 404} by the exception advice.
 */
public class SubmissionNotFoundException extends RuntimeException {

    public SubmissionNotFoundException(String submissionId) {
        super("No submission found for id '" + submissionId + "'");
    }
}
