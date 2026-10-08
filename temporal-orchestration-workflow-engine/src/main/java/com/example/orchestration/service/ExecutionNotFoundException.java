package com.example.orchestration.service;

public class ExecutionNotFoundException extends RuntimeException {

    public ExecutionNotFoundException(String workflowId) {
        super("No workflow execution found with id '" + workflowId + "'");
    }
}
