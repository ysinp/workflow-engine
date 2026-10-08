package com.example.createaccount.dto;

import com.example.createaccount.entity.Account;

/**
 * Converts the JPA entity to the outward-facing DTO. Kept as a tiny
 * standalone component so the entity never leaks past the service layer.
 */
public final class AccountMapper {

    private static final String MASK = "****";

    private AccountMapper() {
    }

    public static AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getFullName(),
                account.getEmail(),
                account.getCreatedAt(),
                account.getNationality(),
                account.getOccupation(),
                account.getAnnualIncome(),
                maskIdNumber(account.getIdNumber()),
                // Pre-existing rows from before the role/balance columns were added have nulls
                // here (see the field comment on Account.role); default them the same way a
                // freshly-created account would have been set up.
                account.getRole() != null ? account.getRole() : "CUSTOMER",
                account.getBalance()
        );
    }

    /**
     * "A1234567" -> "****4567". Null or shorter than 4 chars -> "****" (fully masked).
     */
    static String maskIdNumber(String idNumber) {
        if (idNumber == null || idNumber.length() < 4) {
            return MASK;
        }
        return MASK + idNumber.substring(idNumber.length() - 4);
    }
}
