package com.example.submission.exception;

/**
 * Thrown when the call to create-account's application-staging endpoint fails for any reason
 * other than a well-formed 409 (e.g. connection refused, timeout, unexpected HTTP status).
 * Mapped to 502 Bad Gateway by the exception advice.
 */
public class AccountApplicationServiceException extends RuntimeException {

    public AccountApplicationServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
