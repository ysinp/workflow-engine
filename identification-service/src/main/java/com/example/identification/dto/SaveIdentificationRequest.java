package com.example.identification.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * POST /api/identification body - the wizard's Personal + Contact step.
 * {@code identificationId} is null on the first submission and non-null when the user goes back
 * and re-submits this step - the record is updated in place rather than creating a duplicate.
 * Carries no identification document fields; those are attached by a later PATCH
 * (see {@link UpdateIdentificationRequest}).
 */
public record SaveIdentificationRequest(
        Long identificationId,
        @NotBlank(message = "fullName must not be blank") String fullName,
        @NotBlank(message = "dateOfBirth must not be blank") String dateOfBirth,
        @NotBlank(message = "nationality must not be blank") String nationality,
        String gender,
        @NotBlank(message = "email must not be blank") String email,
        String phone,
        @NotBlank(message = "addressLine1 must not be blank") String addressLine1,
        String addressLine2,
        @NotBlank(message = "city must not be blank") String city,
        String state,
        String postalCode,
        @NotBlank(message = "country must not be blank") String country
) {
}
