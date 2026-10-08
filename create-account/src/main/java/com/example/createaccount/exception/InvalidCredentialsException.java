package com.example.createaccount.exception;

/**
 * Thrown by the service layer when sign-in fails for ANY reason (unknown email or wrong
 * password). A single exception with a single generic message is used on purpose so callers
 * cannot distinguish "no such account" from "wrong password". The controller advice maps this
 * to a 401.
 */
public class InvalidCredentialsException extends RuntimeException {

    public static final String GENERIC_MESSAGE = "Invalid email or password";

    public InvalidCredentialsException() {
        super(GENERIC_MESSAGE);
    }
}
