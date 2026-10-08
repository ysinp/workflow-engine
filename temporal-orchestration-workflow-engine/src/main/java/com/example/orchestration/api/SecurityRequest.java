package com.example.orchestration.api;

import com.example.orchestration.workflow.SecurityData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SecurityRequest(@NotBlank @Size(min = 8) String password) {

    public SecurityData toWorkflowData() {
        return new SecurityData(password);
    }

    @Override
    public String toString() {
        return "SecurityRequest[password=****]";
    }
}
