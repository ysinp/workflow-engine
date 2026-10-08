package com.example.createaccount.dto;

/**
 * Response body for GET /api/accounts/exists?email={email}.
 */
public record ExistsResponse(boolean exists) {
}
