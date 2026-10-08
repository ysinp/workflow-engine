package com.example.orchestration.workflow;

/** Result of an onboarding wizard step Update - {@code nextStep} is the frontend router's slug. */
public record WizardStepResult(Long identificationId, String nextStep) {
}
