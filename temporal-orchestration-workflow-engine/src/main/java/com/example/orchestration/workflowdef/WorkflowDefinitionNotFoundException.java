package com.example.orchestration.workflowdef;

public class WorkflowDefinitionNotFoundException extends RuntimeException {

    public WorkflowDefinitionNotFoundException(String name) {
        super("No saved workflow definition named '" + name + "'");
    }
}
