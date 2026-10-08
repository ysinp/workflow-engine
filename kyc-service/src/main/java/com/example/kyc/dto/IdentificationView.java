package com.example.kyc.dto;

/** Mirrors identification-service's GET /api/identification/{id} response shape. */
public record IdentificationView(
        Long identificationId,
        String fullName,
        String email,
        String idType,
        String idNumber
) {
}
