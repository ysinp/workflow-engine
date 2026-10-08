package com.example.orchestration.workflow;

/** Workflow-level data for the onboarding wizard's Personal + Contact step. */
public record PersonalContactData(
        String fullName,
        String dateOfBirth,
        String nationality,
        String gender,
        String email,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country) {
}
