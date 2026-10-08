package com.example.kyc.dto;

import java.util.List;

/**
 * Response for POST /api/kyc/verify. {@code nextStep} ("security") is present only when
 * {@code verified} is true - the wizard stays on this step, showing {@code reasons}, until
 * verification passes.
 */
public record FinancialVerificationResponse(Long identificationId, boolean verified, List<String> reasons,
                                             String nextStep) {

    private static final String NEXT_STEP = "security";

    public static FinancialVerificationResponse verified(Long identificationId) {
        return new FinancialVerificationResponse(identificationId, true, List.of(), NEXT_STEP);
    }

    public static FinancialVerificationResponse rejected(Long identificationId, List<String> reasons) {
        return new FinancialVerificationResponse(identificationId, false, reasons, null);
    }
}
