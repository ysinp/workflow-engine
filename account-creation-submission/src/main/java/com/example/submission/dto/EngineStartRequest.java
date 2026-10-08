package com.example.submission.dto;

/**
 * Request body sent to {@code POST {base}/api/orchestration/workflows/account-creation/executions}
 * on the temporal-orchestration-workflow-engine service. {@code applicationId} correlates back to
 * the application already staged (via {@link AccountApplicationStartRequest}) in create-account.
 */
public record EngineStartRequest(
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
        String sourceOfFunds
) {
}
