package com.example.kyc.service;

import com.example.kyc.dto.FinancialVerificationRequest;
import com.example.kyc.dto.FinancialVerificationResponse;
import com.example.kyc.dto.IdentificationView;
import org.springframework.stereotype.Service;

@Service
public class FinancialVerificationServiceImpl implements FinancialVerificationService {

    private final IdentificationClient identificationClient;
    private final KycVerificationService verificationService;

    public FinancialVerificationServiceImpl(IdentificationClient identificationClient,
                                             KycVerificationService verificationService) {
        this.identificationClient = identificationClient;
        this.verificationService = verificationService;
    }

    @Override
    public FinancialVerificationResponse verify(FinancialVerificationRequest request) {
        IdentificationView identification = identificationClient.get(request.identificationId());

        KycVerificationResult result = verificationService.verify(
                identification.idType(), identification.idNumber(), request.sourceOfFunds());

        return result.verified()
                ? FinancialVerificationResponse.verified(request.identificationId())
                : FinancialVerificationResponse.rejected(request.identificationId(), result.reasons());
    }
}
