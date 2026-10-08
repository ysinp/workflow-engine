package com.example.createaccount.exception;

import java.util.UUID;

/**
 * Thrown when a lookup/approve/reject targets an application id that doesn't exist.
 * Carries no HTTP knowledge — the controller advice maps this to a 404.
 */
public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(UUID id) {
        super("No application found with id '" + id + "'");
    }
}
