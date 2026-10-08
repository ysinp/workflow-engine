package com.example.orchestration.api;

import com.example.orchestration.service.OnboardingWizardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The sign-up journey's single backend, Product Selection through Security: every step's
 * "what's next" decision is decided by the onboarding wizard workflow (see
 * {@link com.example.orchestration.workflow.OnboardingWizardWorkflow}), not by
 * identification-service, kyc-service, create-account or risk-score directly - the browser never
 * calls those services itself.
 */
@RestController
@RequestMapping("/api/orchestration/onboarding")
public class OnboardingController {

    private final OnboardingWizardService wizardService;

    public OnboardingController(OnboardingWizardService wizardService) {
        this.wizardService = wizardService;
    }

    /** Starts the workflow the moment the product-selection screen loads. */
    @PostMapping
    public ResponseEntity<StartWizardResponse> start() {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(wizardService.start());
    }

    /** Product-selection screen's "Get started" button. */
    @PostMapping("/{workflowId}/product")
    public ProductSelectionResponse submitProduct(@PathVariable String workflowId,
                                                   @Valid @RequestBody ProductSelectionRequest request) {
        return wizardService.submitProductSelection(workflowId, request.toWorkflowData());
    }

    /** Wizard step 1 (Personal + Contact). */
    @PostMapping("/{workflowId}/personal")
    public WizardStepResponse submitPersonal(@PathVariable String workflowId,
                                              @Valid @RequestBody PersonalContactRequest request) {
        return wizardService.submitPersonalContact(workflowId, request.toWorkflowData());
    }

    /** Wizard step 2 (Identification). */
    @PostMapping("/{workflowId}/identification")
    public WizardStepResponse submitIdentification(@PathVariable String workflowId,
                                                     @Valid @RequestBody IdentificationRequest request) {
        return wizardService.submitIdentification(workflowId, request.toWorkflowData());
    }

    /** Wizard step 3 (Financial + KYC verification). */
    @PostMapping("/{workflowId}/financial")
    public FinancialVerificationResponse submitFinancial(@PathVariable String workflowId,
                                                           @Valid @RequestBody FinancialRequest request) {
        return wizardService.submitFinancial(workflowId, request.toWorkflowData());
    }

    /**
     * Wizard step 4 (Security): stages the application and evaluates risk. Never blocks on a
     * reviewer - a HIGH risk verdict returns {@code awaitingReview=true} immediately and the
     * frontend polls {@code GET /api/orchestration/executions/{workflowId}} for the outcome.
     */
    @PostMapping("/{workflowId}/security")
    public AccountCreationStepResponse submitSecurity(@PathVariable String workflowId,
                                                       @Valid @RequestBody SecurityRequest request) {
        return wizardService.submitSecurity(workflowId, request.toWorkflowData());
    }
}
