package com.example.orchestration.api;

import com.example.orchestration.workflow.FinancialData;
import jakarta.validation.constraints.NotBlank;

public record FinancialRequest(String occupation, @NotBlank String annualIncome, @NotBlank String sourceOfFunds) {

    public FinancialData toWorkflowData() {
        return new FinancialData(occupation, annualIncome, sourceOfFunds);
    }
}
