package com.example.orchestration.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Declarative client for kyc-service, used only by {@link
 * com.example.orchestration.activity.OnboardingActivitiesImpl}. The browser no longer calls
 * kyc-service directly - every call is routed through the onboarding wizard workflow.
 */
@FeignClient(name = "kyc-service", url = "${services.kyc-service.base-url}")
public interface KycServiceClient {

    @PostMapping("/api/kyc/verify")
    FinancialVerificationResponse verify(@RequestBody FinancialVerificationRequest request);

    record FinancialVerificationRequest(Long identificationId, String occupation, String annualIncome,
                                         String sourceOfFunds) {
    }

    record FinancialVerificationResponse(Long identificationId, boolean verified, List<String> reasons,
                                          String nextStep) {
    }
}
