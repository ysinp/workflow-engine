package com.example.kyc.service;

import java.util.List;

/** Outcome of {@link KycVerificationService#verify}. */
public record KycVerificationResult(boolean verified, List<String> reasons) {
}
