package com.example.submission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request body for {@code POST /api/account-submissions/{submissionId}/decision}. */
public record SubmitReviewDecisionRequest(
        @NotBlank
        @Pattern(regexp = "APPROVE|REJECT", message = "decision must be APPROVE or REJECT")
        String decision,

        @NotBlank
        String reviewerId,

        String notes
) {
}
