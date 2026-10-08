package com.example.identification.dto;

import com.example.identification.entity.IdentificationRecord;

/**
 * GET /api/identification/{id} response - read by kyc-service (via Feign) to verify the
 * identification document against the Financial step, without sharing a database.
 */
public record IdentificationView(
        Long identificationId,
        String fullName,
        String email,
        String idType,
        String idNumber
) {

    public static IdentificationView from(IdentificationRecord record) {
        return new IdentificationView(record.getId(), record.getFullName(), record.getEmail(),
                record.getIdType(), record.getIdNumber());
    }
}
