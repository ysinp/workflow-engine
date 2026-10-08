package com.example.createaccount.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound payload for POST /api/accounts/signin. {@code password} is plaintext.
 */
public record SignInRequest(
        @NotBlank(message = "email must not be blank") String email,
        @NotBlank(message = "password must not be blank") String password
) {

    /** Keeps the plaintext password out of logs if this record is ever printed. */
    @Override
    public String toString() {
        return "SignInRequest[email=" + email + ", password=****]";
    }
}
