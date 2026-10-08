package com.example.submission.exception;

/**
 * Thrown when create-account rejects a new application because the email already has an
 * account, or an application already pending. Mapped to 409 with code DUPLICATE_EMAIL by the
 * exception advice - deliberately the same code the old direct-signup flow used, so the
 * frontend's existing duplicate-email handling needs no changes.
 */
public class DuplicateApplicationException extends RuntimeException {

    public DuplicateApplicationException(String message) {
        super(message);
    }
}
