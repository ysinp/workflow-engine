package com.example.submission.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound request body for {@code POST /api/account-submissions}. This is the single entry
 * point for a KYC signup: it stages the application in create-account (password BCrypt-hashed
 * there) and starts the risk-assessment workflow that will approve or reject it. No Account row
 * exists, and its owner cannot sign in, until that workflow reaches a final decision.
 */
public record SubmitAccountRequest(

        @NotBlank(message = "fullName must not be blank")
        String fullName,

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email,

        @NotBlank(message = "password must not be blank")
        @Size(min = 8, message = "password must be at least 8 characters")
        String password,

        String dateOfBirth,
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
        return "SubmitAccountRequest[fullName=" + fullName + ", email=" + email + ", password=****]";
    }
}
