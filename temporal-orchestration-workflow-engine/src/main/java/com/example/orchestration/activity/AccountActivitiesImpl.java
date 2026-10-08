package com.example.orchestration.activity;

import com.example.orchestration.client.CreateAccountClient;
import com.example.orchestration.client.RiskScoreClient;
import com.example.orchestration.client.RiskScoreClient.RiskEvaluationRequest;
import com.example.orchestration.client.RiskScoreClient.RiskEvaluationResponse;
import com.example.orchestration.workflow.AccountCreationInput;
import com.example.orchestration.workflow.StageApplicationData;
import feign.FeignException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.temporal.failure.ApplicationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * All I/O for the workflow lives here. Activity code may do normal I/O and logging; any exception
 * thrown is reported to Temporal, which retries according to the stub's RetryOptions.
 *
 * <p>Business-outcome metrics are recorded here rather than in the workflow method: activities
 * execute exactly once per workflow run (Temporal caches their result in history), whereas the
 * workflow method itself re-runs on every replay - incrementing a counter there would double-count
 * every time the workflow is replayed.
 */
@Component
public class AccountActivitiesImpl implements AccountActivities {

    private static final Logger log = LoggerFactory.getLogger(AccountActivitiesImpl.class);

    static final String CREATE_ACCOUNT_SERVICE_ERROR = "CreateAccountServiceError";
    static final String RISK_SCORE_SERVICE_ERROR = "RiskScoreServiceError";

    private final CreateAccountClient createAccountClient;
    private final RiskScoreClient riskScoreClient;
    private final Counter approvedCounter;
    private final Counter rejectedCounter;

    public AccountActivitiesImpl(CreateAccountClient createAccountClient,
                                  RiskScoreClient riskScoreClient,
                                  MeterRegistry meterRegistry) {
        this.createAccountClient = createAccountClient;
        this.riskScoreClient = riskScoreClient;
        this.approvedCounter = Counter.builder("account_creation_outcomes_total")
                .tag("outcome", "approved")
                .description("Risk-assessment workflow executions that resulted in approval")
                .register(meterRegistry);
        this.rejectedCounter = Counter.builder("account_creation_outcomes_total")
                .tag("outcome", "rejected")
                .description("Risk-assessment workflow executions that resulted in rejection")
                .register(meterRegistry);
    }

    @Override
    public boolean checkSubmission(String email) {
        try {
            CreateAccountClient.ExistsResponse response = createAccountClient.exists(email);
            return response != null && response.exists();
        } catch (FeignException e) {
            throw serviceFailure("GET /api/accounts/exists", CREATE_ACCOUNT_SERVICE_ERROR, e);
        }
    }

    @Override
    public String stageApplication(StageApplicationData data) {
        try {
            CreateAccountClient.StageApplicationResponse response = createAccountClient.submit(
                    new CreateAccountClient.StageApplicationRequest(
                            data.fullName(), data.email(), data.password(), data.dateOfBirth(), data.nationality(),
                            data.phone(), data.addressLine1(), data.addressLine2(), data.city(), data.state(),
                            data.postalCode(), data.country(), data.idType(), data.idNumber(), data.occupation(),
                            data.annualIncome(), data.sourceOfFunds()));
            log.info("Staged application {} for {}", response.applicationId(), data.email());
            return response.applicationId();
        } catch (FeignException e) {
            throw serviceFailure("POST /api/account-applications", CREATE_ACCOUNT_SERVICE_ERROR, e);
        }
    }

    @Override
    public RiskAssessment evaluateRisk(AccountCreationInput input) {
        try {
            RiskEvaluationResponse response = riskScoreClient.evaluate(RiskEvaluationRequest.from(input));
            log.info("Risk assessment for {}: level={} score={}", input.email(), response.riskLevel(), response.score());
            return new RiskAssessment(response.riskLevel(), response.score(), response.reasons());
        } catch (FeignException e) {
            throw serviceFailure("POST /api/risk-score/evaluate", RISK_SCORE_SERVICE_ERROR, e);
        }
    }

    @Override
    public void activateAccount(String applicationId) {
        try {
            createAccountClient.approveApplication(applicationId);
        } catch (FeignException e) {
            throw serviceFailure("POST /api/account-applications/" + applicationId + "/approve",
                    CREATE_ACCOUNT_SERVICE_ERROR, e);
        }
    }

    @Override
    public void rejectApplication(String applicationId) {
        try {
            createAccountClient.rejectApplication(applicationId);
        } catch (FeignException e) {
            throw serviceFailure("POST /api/account-applications/" + applicationId + "/reject",
                    CREATE_ACCOUNT_SERVICE_ERROR, e);
        }
    }

    @Override
    public void notifyApproved(String email, String fullName) {
        // SIMULATED notification (POC) - logged, not sent.
        log.info("[SIMULATED EMAIL] to={} subject=\"{}\" body=\"{}\"",
                email,
                "Your KYC application has been approved",
                "Hi " + fullName + ", your application has passed our risk assessment.");
        approvedCounter.increment();
    }

    @Override
    public void notifyRejected(String email, String fullName, List<String> reasons) {
        // SIMULATED notification (POC) - logged, not sent.
        log.info("[SIMULATED EMAIL] to={} subject=\"{}\" body=\"{}\" reasons={}",
                email,
                "Your KYC application requires further review",
                "Hi " + fullName + ", your application did not clear our automated risk assessment.",
                reasons);
        rejectedCounter.increment();
    }

    /** Retryable failure so the workflow's RetryOptions apply. */
    private static ApplicationFailure serviceFailure(String call, String type, FeignException e) {
        return ApplicationFailure.newFailureWithCause(
                "service call " + call + " failed with HTTP " + e.status() + ": " + e.contentUTF8(),
                type,
                e);
    }
}
