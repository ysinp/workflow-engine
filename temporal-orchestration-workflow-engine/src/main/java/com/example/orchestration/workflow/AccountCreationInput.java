package com.example.orchestration.workflow;

/**
 * Workflow input; serialized by Temporal's default (Jackson) data converter.
 *
 * <p>Deliberately carries no password/credential material - the password is BCrypt-hashed and
 * stored by create-account at staging time, before this workflow ever starts, so nothing secret
 * ever needs to be recorded in Temporal's workflow history.
 *
 * <p>{@code applicationId} correlates back to the staged application in create-account
 * (see {@code account_applications}) - {@link AccountCreationWorkflowImpl} uses it to activate
 * (create the real Account row) or reject that application once risk is decided. The account
 * does not exist, and its owner cannot sign in, until this workflow reaches that outcome.
 */
public record AccountCreationInput(
        String applicationId,
        String fullName,
        String email,
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
}
