package com.example.orchestration.workflowdef;

import java.time.Instant;

/** The list endpoint's row shape - omits the (potentially large) definition JSON. */
public record WorkflowDefinitionSummary(Long id, String name, Instant updatedAt, String updatedBy) {

    static WorkflowDefinitionSummary from(WorkflowDefinition definition) {
        return new WorkflowDefinitionSummary(
                definition.getId(), definition.getName(), definition.getUpdatedAt(), definition.getUpdatedBy());
    }
}
