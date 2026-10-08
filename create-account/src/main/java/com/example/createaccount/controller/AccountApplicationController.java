package com.example.createaccount.controller;

import com.example.createaccount.dto.ApplicationStatusResponse;
import com.example.createaccount.dto.SubmitApplicationRequest;
import com.example.createaccount.service.AccountApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Staged KYC applications. {@code submit}/{@code getStatus} are called by the frontend
 * (indirectly, via account-creation-submission); {@code approve}/{@code reject} are called
 * service-to-service by the orchestration engine's activities once a risk decision is final -
 * same "internal, unauthenticated" convention already used by {@code GET /api/accounts/exists}.
 */
@RestController
@RequestMapping("/api/account-applications")
public class AccountApplicationController {

    private final AccountApplicationService applicationService;

    public AccountApplicationController(AccountApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApplicationStatusResponse> submit(@Valid @RequestBody SubmitApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(applicationService.submit(request));
    }

    @GetMapping("/{id}")
    public ApplicationStatusResponse getStatus(@PathVariable UUID id) {
        return applicationService.getStatus(id);
    }

    @PostMapping("/{id}/approve")
    public ApplicationStatusResponse approve(@PathVariable UUID id) {
        return applicationService.approve(id);
    }

    @PostMapping("/{id}/reject")
    public ApplicationStatusResponse reject(@PathVariable UUID id) {
        return applicationService.reject(id);
    }
}
