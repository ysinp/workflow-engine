package com.example.kyc.exception;

/** Uniform error body shape: { "code": "...", "message": "..." } */
public record ErrorResponse(String code, String message) {
}
