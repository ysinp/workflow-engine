package com.example.createaccount.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Inbound payload for POST /api/account-applications. Same shape as the old direct-signup
 * request - only where it lands differs (a staged application, not an Account row).
 */
public record SubmitApplicationRequest(
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
        return "SubmitApplicationRequest[fullName=" + fullName + ", email=" + email + ", password=****]";
    }
}
