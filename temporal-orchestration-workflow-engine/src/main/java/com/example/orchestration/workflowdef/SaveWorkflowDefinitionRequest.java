package com.example.orchestration.workflowdef;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @param name       identifies the diagram for later save/load - the Workflow Editor sends the
 *                   exported diagram's own {@code name} field (e.g. "account-creation-workflow")
 * @param definition the full exported diagram JSON verbatim (nodes, edges, layoutDirection, ...)
 */
public record SaveWorkflowDefinitionRequest(
        @NotBlank String name,
        @NotNull JsonNode definition) {
}
