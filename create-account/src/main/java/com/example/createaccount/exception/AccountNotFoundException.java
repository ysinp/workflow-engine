package com.example.createaccount.exception;

import java.util.UUID;

/**
 * Thrown by the service layer when a lookup by id finds no account.
 * Carries no HTTP knowledge — the controller advice maps this to a 404.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID id) {
        super("No account found with id '" + id + "'");
    }
}
