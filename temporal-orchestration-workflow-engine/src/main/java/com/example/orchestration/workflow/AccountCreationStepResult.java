package com.example.orchestration.workflow;

import java.util.List;

/**
 * Result of the onboarding wizard's Security step Update ({@link OnboardingWizardWorkflow#submitSecurity}).
 * This call never blocks on a human reviewer: a LOW risk verdict auto-approves and returns the
 * final {@code result} synchronously; a HIGH risk verdict returns immediately with
 * {@code awaitingReview=true} and a null {@code result} - the caller then polls the workflow's
 * execution status (see {@code WorkflowExecutionService}) for the eventual reviewer decision.
 */
public record AccountCreationStepResult(String riskLevel, int score, List<String> reasons, boolean awaitingReview,
                                        AccountCreationResult result) {
}
