package com.example.createaccount.exception;

/**
 * Uniform error body shape: { "code": "...", "message": "..." }
 */
public record ErrorResponse(String code, String message) {
}
