package com.example.orchestration.api;

import com.example.orchestration.activity.RiskAssessment;
import com.example.orchestration.service.WorkflowExecutionService.ExecutionState;
import com.example.orchestration.workflow.AccountCreationResult;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * {@code { workflowId, status, result?, errorMessage?, pendingRiskAssessment? }} - absent fields
 * are omitted. {@code pendingRiskAssessment} is only present when {@code status} is
 * {@code AWAITING_REVIEW}, giving a reviewer the context needed to decide.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExecutionStatusResponse(String workflowId, String status, AccountCreationResult result,
                                      String errorMessage, RiskAssessment pendingRiskAssessment) {

    static ExecutionStatusResponse from(ExecutionState state) {
        return new ExecutionStatusResponse(state.workflowId(), state.status(), state.result(),
                state.errorMessage(), state.pendingRiskAssessment());
    }
}
