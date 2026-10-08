package com.example.submission.dto;

/**
 * Response body (HTTP 202) from the orchestration engine's start-execution endpoint.
 */
public record EngineStartResponse(
        String workflowId,
        String runId,
        String status
) {
}
