package com.example.orchestration.workflow;

/** Workflow-level data for the onboarding wizard's Identification step. */
public record IdentificationData(String idType, String idNumber) {
}
