package com.example.orchestration.service;

/** No bearer token, an invalid/expired one, or one whose role claim isn't REVIEWER. */
public class ReviewerAccessDeniedException extends RuntimeException {

    public ReviewerAccessDeniedException(String message) {
        super(message);
    }
}
