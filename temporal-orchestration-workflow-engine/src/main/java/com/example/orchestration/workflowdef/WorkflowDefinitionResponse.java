package com.example.orchestration.workflowdef;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record WorkflowDefinitionResponse(
        Long id,
        String name,
        JsonNode definition,
        Instant updatedAt,
        String updatedBy) {
}
