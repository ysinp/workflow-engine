package com.example.kyc.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Demo-scope completeness/format verification of the identification document (from
 * identification-service) against the financial profile (this step). Deliberately distinct from
 * risk-score's job: this checks "is the submitted KYC data well-formed", not "is this applicant
 * risky" - the two concerns stay in separate services, same as the rest of this POC's boundaries.
 */
@Service
public class KycVerificationService {

    private static final Set<String> KNOWN_ID_TYPES = Set.of("PASSPORT", "NATIONAL_ID", "DRIVERS_LICENSE");
    private static final Set<String> KNOWN_SOURCES_OF_FUNDS =
            Set.of("EMPLOYMENT", "BUSINESS", "INVESTMENTS", "INHERITANCE", "OTHER");

    public KycVerificationResult verify(String idType, String idNumber, String sourceOfFunds) {
        List<String> reasons = new ArrayList<>();

        if (idType == null || !KNOWN_ID_TYPES.contains(idType.toUpperCase())) {
            reasons.add("Identification document type is not recognized");
        }
        if (idNumber == null || idNumber.trim().length() < 4 || idNumber.chars().noneMatch(Character::isDigit)) {
            reasons.add("Identification document number does not look well-formed");
        }
        if (sourceOfFunds == null || !KNOWN_SOURCES_OF_FUNDS.contains(sourceOfFunds.toUpperCase())) {
            reasons.add("Source of funds is not recognized");
        }

        return new KycVerificationResult(reasons.isEmpty(), reasons);
    }
}
