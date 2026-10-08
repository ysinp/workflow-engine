package com.example.submission.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Uniform error body shape: {@code { "code", "message", "fieldErrors"? } }.
 * {@code fieldErrors} is only present for validation failures.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, Map<String, String> fieldErrors) {

    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }
}
