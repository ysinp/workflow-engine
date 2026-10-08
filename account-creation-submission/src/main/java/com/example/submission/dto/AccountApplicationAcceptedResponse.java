package com.example.submission.dto;

/**
 * Response body from create-account's {@code POST /api/account-applications}.
 * {@code accountId} is always null at this point - the application has only just been staged.
 */
public record AccountApplicationAcceptedResponse(String applicationId, String status, String accountId) {
}
