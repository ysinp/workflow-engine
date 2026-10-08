package com.example.createaccount.controller;

import com.example.createaccount.dto.AccountResponse;
import com.example.createaccount.dto.CreateAccountRequest;
import com.example.createaccount.dto.ExistsResponse;
import com.example.createaccount.dto.SignInRequest;
import com.example.createaccount.dto.SignInResponse;
import com.example.createaccount.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST surface for accounts: KYC signup, sign-in (JWT issuance), and authenticated reads.
 * No business logic lives here — everything is delegated to the AccountService interface.
 * Which endpoints require a JWT is decided in {@code config.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** Public (service-to-service). */
    @GetMapping("/exists")
    public ResponseEntity<ExistsResponse> exists(@RequestParam String email) {
        return ResponseEntity.ok(new ExistsResponse(accountService.emailExists(email)));
    }

    /** Public: signup. Accepts a plaintext password, which the service BCrypt-hashes. */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Public: verifies credentials and returns a JWT plus the account view. */
    @PostMapping("/signin")
    public ResponseEntity<SignInResponse> signIn(@Valid @RequestBody SignInRequest request) {
        return ResponseEntity.ok(accountService.signIn(request));
    }

    /** Requires a valid bearer JWT. */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    /** Requires a valid bearer JWT. */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }
}
