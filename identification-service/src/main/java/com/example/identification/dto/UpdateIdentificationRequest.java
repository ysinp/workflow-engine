package com.example.identification.dto;

import jakarta.validation.constraints.NotBlank;

/** PATCH /api/identification/{id} body - the wizard's own, separate Identification step. */
public record UpdateIdentificationRequest(
        @NotBlank(message = "idType must not be blank") String idType,
        @NotBlank(message = "idNumber must not be blank") String idNumber
) {
}
