package com.example.riskscore.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound payload for POST /api/risk-score/evaluate.
 * This is a stateless scoring call, not a form validator: only {@code email} and
 * {@code idNumber} are required, everything else may be blank or null. Enum-shaped
 * fields (nationality/country, idType, annualIncome, sourceOfFunds) are accepted as
 * plain strings and matched against rule constants server-side - an unrecognized
 * value simply fails to match any rule rather than being rejected as invalid.
 */
public record RiskEvaluationRequest(
        String fullName,
        @NotBlank(message = "email must not be blank") String email,
        String dateOfBirth,
        String nationality,
        String idType,
        @NotBlank(message = "idNumber must not be blank") String idNumber,
        String occupation,
        String annualIncome,
        String sourceOfFunds,
        Address address
) {

    public record Address(
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country
    ) {
    }
}
