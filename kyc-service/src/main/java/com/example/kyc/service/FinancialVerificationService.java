package com.example.kyc.service;

import com.example.kyc.dto.FinancialVerificationRequest;
import com.example.kyc.dto.FinancialVerificationResponse;

public interface FinancialVerificationService {

    FinancialVerificationResponse verify(FinancialVerificationRequest request);
}
