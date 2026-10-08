package com.example.createaccount.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Outbound account representation for the dashboard view.
 * Security by design: deliberately minimal — never exposes passwordHash, the full idNumber
 * (only a masked last-4), or address/contact/DOB fields.
 */
public record AccountResponse(
        UUID id,
        String fullName,
        String email,
        Instant createdAt,
        String nationality,
        String occupation,
        String annualIncome,
        String idNumberMasked,
        String role,
        BigDecimal balance
) {
}
