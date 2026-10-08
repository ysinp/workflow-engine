package com.example.kyc.controller;

import com.example.kyc.dto.FinancialVerificationRequest;
import com.example.kyc.dto.FinancialVerificationResponse;
import com.example.kyc.service.FinancialVerificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kyc")
public class KycController {

    private final FinancialVerificationService financialVerificationService;

    public KycController(FinancialVerificationService financialVerificationService) {
        this.financialVerificationService = financialVerificationService;
    }

    /** Wizard step 2 (Financial + KYC verification). */
    @PostMapping("/verify")
    public FinancialVerificationResponse verify(@Valid @RequestBody FinancialVerificationRequest request) {
        return financialVerificationService.verify(request);
    }
}
