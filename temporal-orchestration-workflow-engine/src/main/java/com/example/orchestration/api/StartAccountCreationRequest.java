package com.example.orchestration.api;

import com.example.orchestration.workflow.AccountCreationInput;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * REST request body; kept separate from the workflow input so validation stays at the edge.
 * Deliberately carries no password - the caller (account-creation-submission) has already staged
 * the application (with its BCrypt-hashed password) in create-account before this endpoint is
 * ever called, and passes back only {@code applicationId} for correlation.
 */
public record StartAccountCreationRequest(
        @NotBlank String applicationId,
        @NotBlank String fullName,
        @NotBlank @Email String email,
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
        String sourceOfFunds) {

    public AccountCreationInput toWorkflowInput() {
        return new AccountCreationInput(applicationId, fullName, email, dateOfBirth, nationality, phone,
                addressLine1, addressLine2, city, state, postalCode, country,
                idType, idNumber, occupation, annualIncome, sourceOfFunds);
    }
}
