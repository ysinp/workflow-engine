package com.example.identification.dto;

/** Shared response shape for both POST (create/update Personal+Contact) and PATCH (Identification). */
public record DraftResponse(Long identificationId, String nextStep) {
}
