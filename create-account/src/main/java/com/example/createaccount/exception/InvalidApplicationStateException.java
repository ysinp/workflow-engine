package com.example.createaccount.exception;

/**
 * Thrown when approve/reject is called on an application already decided the OTHER way (e.g.
 * rejecting one already APPROVED). Retrying the SAME decision on an already-decided application
 * is deliberately idempotent (not an error) - Temporal activities execute at-least-once, so the
 * activate/reject activity may be retried after its own response was lost in transit.
 * Carries no HTTP knowledge — the controller advice maps this to a 409.
 */
public class InvalidApplicationStateException extends RuntimeException {

    public InvalidApplicationStateException(String message) {
        super(message);
    }
}
