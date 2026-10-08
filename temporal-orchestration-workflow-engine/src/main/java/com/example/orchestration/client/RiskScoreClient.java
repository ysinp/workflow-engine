package com.example.orchestration.client;

import com.example.orchestration.workflow.AccountCreationInput;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/** Declarative client for the risk-score service, used only by the activity implementation. */
@FeignClient(name = "risk-score", url = "${services.risk-score.base-url}")
public interface RiskScoreClient {

    @PostMapping("/api/risk-score/evaluate")
    RiskEvaluationResponse evaluate(@RequestBody RiskEvaluationRequest request);

    record RiskEvaluationRequest(
            String fullName, String email, String dateOfBirth, String nationality,
            String idType, String idNumber, String occupation, String annualIncome,
            String sourceOfFunds, Address address) {

        public static RiskEvaluationRequest from(AccountCreationInput input) {
            return new RiskEvaluationRequest(
                    input.fullName(), input.email(), input.dateOfBirth(), input.nationality(),
                    input.idType(), input.idNumber(), input.occupation(), input.annualIncome(),
                    input.sourceOfFunds(),
                    new Address(input.addressLine1(), input.addressLine2(), input.city(),
                            input.state(), input.postalCode(), input.country()));
        }
    }

    record Address(String line1, String line2, String city, String state, String postalCode, String country) {
    }

    record RiskEvaluationResponse(String riskLevel, int score, List<String> reasons) {
    }
}
