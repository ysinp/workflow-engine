package com.example.orchestration.workflow;

/**
 * Workflow-level data for the onboarding wizard's Security step. Carries only the password - every
 * other field needed to stage the application and evaluate risk was already collected and stored
 * by the earlier Personal/Identification/Financial steps (see {@link OnboardingWizardWorkflowImpl}).
 */
public record SecurityData(String password) {

    @Override
    public String toString() {
        return "SecurityData[password=****]";
    }
}
