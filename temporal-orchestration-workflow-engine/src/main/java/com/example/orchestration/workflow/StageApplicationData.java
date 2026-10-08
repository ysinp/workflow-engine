package com.example.orchestration.workflow;

/**
 * Full KYC profile + password needed to stage a new application in create-account (password
 * BCrypt-hashed there; never retried by Temporal since this crosses into
 * {@link com.example.orchestration.activity.AccountActivities#stageApplication}, a single
 * activity call under the usual RetryOptions - the password itself never lingers in workflow
 * history beyond this one input to that activity).
 */
public record StageApplicationData(
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
        String sourceOfFunds) {

    @Override
    public String toString() {
        return "StageApplicationData[fullName=" + fullName + ", email=" + email + ", password=****]";
    }
}
