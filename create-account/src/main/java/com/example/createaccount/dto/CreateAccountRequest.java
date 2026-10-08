package com.example.createaccount.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Inbound payload for POST /api/accounts (KYC signup).
 * <p>
 * {@code password} is PLAINTEXT, sent directly by the frontend over HTTPS. This service hashes
 * it with BCrypt before persisting; the plaintext is never stored or logged. Only
 * fullName/email/password are required — the KYC fields are intentionally optional for this demo.
 * <p>
 * {@code dateOfBirth} is ISO-8601 ({@code "YYYY-MM-DD"}).
 */
public record CreateAccountRequest(
        @NotBlank(message = "fullName must not be blank") String fullName,
        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address") String email,
        @NotBlank(message = "password must not be blank")
        @Size(min = 8, message = "password must be at least 8 characters") String password,
        LocalDate dateOfBirth,
        String nationality,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        String idType,
        String idNumber,
        String occupation,
        String annualIncome,
        String sourceOfFunds
) {

    /** Keeps the plaintext password out of logs/exception messages if this record is ever printed. */
    @Override
    public String toString() {
        return "CreateAccountRequest[fullName=" + fullName + ", email=" + email + ", password=****]";
    }
}
