package com.example.orchestration.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Declarative client for the create-account service, used only by the activity implementation. */
@FeignClient(name = "create-account", url = "${services.create-account.base-url}")
public interface CreateAccountClient {

    @GetMapping("/api/accounts/exists")
    ExistsResponse exists(@RequestParam("email") String email);

    /** Stages a new KYC submission (password BCrypt-hashed there) ahead of risk evaluation. */
    @PostMapping("/api/account-applications")
    StageApplicationResponse submit(@RequestBody StageApplicationRequest request);

    /** Creates the real Account row from a staged application. */
    @PostMapping("/api/account-applications/{applicationId}/approve")
    void approveApplication(@PathVariable("applicationId") String applicationId);

    /** Marks a staged application rejected; no Account row is ever created for it. */
    @PostMapping("/api/account-applications/{applicationId}/reject")
    void rejectApplication(@PathVariable("applicationId") String applicationId);

    record ExistsResponse(boolean exists) {
    }

    record StageApplicationRequest(
            String fullName,
            String email,
            String password,
            String dateOfBirth,
            String nationality,
            String phone,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            String idType,
            String idNumber,
            String occupation,
            String annualIncome,
            String sourceOfFunds) {

        @Override
        public String toString() {
            return "StageApplicationRequest[fullName=" + fullName + ", email=" + email + ", password=****]";
        }
    }

    /** {@code accountId} is always null at this point - the application has only just been staged. */
    record StageApplicationResponse(String applicationId, String status, String accountId) {
    }
}
