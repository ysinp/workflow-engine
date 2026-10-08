package com.example.orchestration.api;

import com.example.orchestration.workflow.ProductSelectionData;
import jakarta.validation.constraints.NotBlank;

/** @param product Presentational only - the product picked on the sign-up product-selection screen. */
public record ProductSelectionRequest(@NotBlank String product) {

    public ProductSelectionData toWorkflowData() {
        return new ProductSelectionData(product);
    }
}
