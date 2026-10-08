package com.example.orchestration.workflow;

/** Final result once the onboarding wizard's data-collection steps are all complete. */
public record OnboardingWizardResult(Long identificationId, String finalStep) {
}
