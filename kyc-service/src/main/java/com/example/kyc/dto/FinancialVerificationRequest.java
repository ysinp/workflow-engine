package com.example.kyc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** POST /api/kyc/verify body - the wizard's Financial step, verified against an identification-service record. */
public record FinancialVerificationRequest(
        @NotNull(message = "identificationId must not be null") Long identificationId,
        String occupation,
        @NotBlank(message = "annualIncome must not be blank") String annualIncome,
        @NotBlank(message = "sourceOfFunds must not be blank") String sourceOfFunds
) {
}
