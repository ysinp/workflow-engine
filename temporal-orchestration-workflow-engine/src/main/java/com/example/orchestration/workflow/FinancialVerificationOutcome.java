package com.example.orchestration.workflow;

import java.util.List;

/**
 * Result of the onboarding wizard's Financial step Update. {@code nextStep} is present only when
 * {@code verified} is true - absent means the wizard stays on this step.
 */
public record FinancialVerificationOutcome(Long identificationId, boolean verified, List<String> reasons,
                                            String nextStep) {
}
