package com.example.submission.exception;

/**
 * Thrown when the call to the orchestration engine fails for any reason other than a
 * well-formed 404 (e.g. connection refused, timeout, or an unexpected HTTP status).
 * Mapped to {@code 502 Bad Gateway} by the exception advice - the raw cause is logged
 * server-side but never leaked to the client.
 */
public class OrchestrationEngineException extends RuntimeException {

    public OrchestrationEngineException(String message) {
        super(message);
    }

    public OrchestrationEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
