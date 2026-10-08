package com.example.kyc.exception;

/** identification-service could not be reached, or responded unexpectedly. */
public class IdentificationServiceUnavailableException extends RuntimeException {

    public IdentificationServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
