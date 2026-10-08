package com.example.submission.dto;

/**
 * Request body sent to {@code POST {base}/api/account-applications} on the create-account
 * service - stages the KYC submission (password BCrypt-hashed there) before any workflow starts.
 */
public record AccountApplicationStartRequest(
        String fullName,
        String email,
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

    /** Keeps the plaintext password out of logs if this record is ever printed. */
    @Override
    public String toString() {
        return "AccountApplicationStartRequest[fullName=" + fullName + ", email=" + email + ", password=****]";
    }
}
