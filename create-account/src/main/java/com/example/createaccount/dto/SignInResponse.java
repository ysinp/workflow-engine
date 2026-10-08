package com.example.createaccount.dto;

/**
 * Response body for a successful POST /api/accounts/signin.
 */
public record SignInResponse(String token, AccountResponse account) {
}
