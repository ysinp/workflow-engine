package com.example.orchestration.api;

import com.example.orchestration.workflow.IdentificationData;
import jakarta.validation.constraints.NotBlank;

public record IdentificationRequest(@NotBlank String idType, @NotBlank String idNumber) {

    public IdentificationData toWorkflowData() {
        return new IdentificationData(idType, idNumber);
    }
}
