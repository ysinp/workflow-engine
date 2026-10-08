package com.example.createaccount.dto;

import java.util.UUID;

/**
 * {@code { applicationId, status, accountId? } } - accountId is present only once status is
 * APPROVED. Returned by the submit call (202), the polling GET, and the approve/reject calls.
 */
public record ApplicationStatusResponse(UUID applicationId, String status, UUID accountId) {
}
