package com.example.orchestration.workflow;

/** Workflow-level data for the onboarding wizard's Financial step. */
public record FinancialData(String occupation, String annualIncome, String sourceOfFunds) {
}
