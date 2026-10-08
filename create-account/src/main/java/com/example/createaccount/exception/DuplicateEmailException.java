package com.example.createaccount.exception;

/**
 * Thrown by the service layer when an account is created with an email
 * that already exists. Carries no HTTP knowledge — the controller advice
 * maps this to a 409.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}
