package com.example.orchestration.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Declarative client for identification-service, used only by {@link
 * com.example.orchestration.activity.OnboardingActivitiesImpl}. The browser no longer calls
 * identification-service directly - every call is routed through the onboarding wizard workflow.
 */
@FeignClient(name = "identification-service", url = "${services.identification-service.base-url}")
public interface IdentificationServiceClient {

    @PostMapping("/api/identification")
    DraftResponse save(@RequestBody SaveIdentificationRequest request);

    @PatchMapping("/api/identification/{identificationId}")
    DraftResponse updateIdentification(@PathVariable("identificationId") Long identificationId,
                                        @RequestBody UpdateIdentificationRequest request);

    record SaveIdentificationRequest(
            Long identificationId,
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

    record UpdateIdentificationRequest(String idType, String idNumber) {
    }

    record DraftResponse(Long identificationId, String nextStep) {
    }
}
