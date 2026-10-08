package com.example.orchestration.activity;

import com.example.orchestration.client.CreateAccountClient;
import com.example.orchestration.client.RiskScoreClient;
import com.example.orchestration.workflow.AccountCreationInput;
import com.example.orchestration.workflow.StageApplicationData;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.temporal.failure.ApplicationFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies the activity implementation against mocked Feign clients (no real HTTP). */
@ExtendWith(MockitoExtension.class)
class AccountActivitiesImplTest {

    private static final AccountCreationInput INPUT = new AccountCreationInput(
            "app-1", "Ada", "ada@example.com", "1990-01-01", "US", null,
            null, null, null, null, null, null, "PASSPORT", "A1234567", null, null, null);

    @Mock
    private CreateAccountClient createAccountClient;
    @Mock
    private RiskScoreClient riskScoreClient;

    private AccountActivitiesImpl activities;

    @BeforeEach
    void setUp() {
        activities = new AccountActivitiesImpl(createAccountClient, riskScoreClient, new SimpleMeterRegistry());
    }

    @Test
    void checkSubmissionParsesExistsFlag() {
        when(createAccountClient.exists("ada+test@example.com"))
                .thenReturn(new CreateAccountClient.ExistsResponse(true));

        assertThat(activities.checkSubmission("ada+test@example.com")).isTrue();
    }

    @Test
    void stageApplicationReturnsApplicationId() {
        when(createAccountClient.submit(any())).thenReturn(
                new CreateAccountClient.StageApplicationResponse("app-1", "STAGED", null));
        StageApplicationData data = new StageApplicationData(
                "Ada", "ada@example.com", "Sup3rSecret!", "1990-01-01", "US", null,
                null, null, null, null, null, null, "PASSPORT", "A1234567", null, null, null);

        assertThat(activities.stageApplication(data)).isEqualTo("app-1");
    }

    @Test
    void stageApplicationNon2xxThrowsRetryableApplicationFailure() {
        when(createAccountClient.submit(any())).thenThrow(feignErrorWithBody(503, "create-account down"));
        StageApplicationData data = new StageApplicationData(
                "Ada", "ada@example.com", "Sup3rSecret!", "1990-01-01", "US", null,
                null, null, null, null, null, null, "PASSPORT", "A1234567", null, null, null);

        assertThatThrownBy(() -> activities.stageApplication(data))
                .isInstanceOfSatisfying(ApplicationFailure.class,
                        f -> assertThat(f.getType()).isEqualTo(AccountActivitiesImpl.CREATE_ACCOUNT_SERVICE_ERROR));
    }

    @Test
    void evaluateRiskPassesInputThroughAndParsesResponse() {
        when(riskScoreClient.evaluate(any())).thenReturn(new RiskScoreClient.RiskEvaluationResponse(
                "HIGH", 100, List.of("Nationality US is on the high-risk jurisdiction list")));

        RiskAssessment result = activities.evaluateRisk(INPUT);

        assertThat(result.riskLevel()).isEqualTo("HIGH");
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.reasons()).containsExactly("Nationality US is on the high-risk jurisdiction list");
    }

    @Test
    void evaluateRiskNon2xxThrowsRetryableApplicationFailure() {
        when(riskScoreClient.evaluate(any())).thenThrow(feignErrorWithBody(500, "risk-score down"));

        assertThatThrownBy(() -> activities.evaluateRisk(INPUT))
                .isInstanceOfSatisfying(ApplicationFailure.class, f -> {
                    assertThat(f.isNonRetryable()).isFalse();
                    assertThat(f.getType()).isEqualTo(AccountActivitiesImpl.RISK_SCORE_SERVICE_ERROR);
                    assertThat(f.getOriginalMessage()).contains("HTTP 500").contains("risk-score down");
                });
    }

    @Test
    void activateAccountCallsApprove() {
        activities.activateAccount("app-1");

        verify(createAccountClient).approveApplication(eq("app-1"));
    }

    @Test
    void activateAccountNon2xxThrowsRetryableApplicationFailure() {
        FeignException failure = feignErrorWithBody(502, "create-account down");
        org.mockito.Mockito.doThrow(failure).when(createAccountClient).approveApplication(any());

        assertThatThrownBy(() -> activities.activateAccount("app-1"))
                .isInstanceOfSatisfying(ApplicationFailure.class,
                        f -> assertThat(f.getType()).isEqualTo(AccountActivitiesImpl.CREATE_ACCOUNT_SERVICE_ERROR));
    }

    @Test
    void rejectApplicationCallsReject() {
        activities.rejectApplication("app-1");

        verify(createAccountClient).rejectApplication(eq("app-1"));
    }

    private static FeignException feignErrorWithBody(int status, String body) {
        Request request = Request.create(Request.HttpMethod.POST, "/api/x",
                Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
        Response response = Response.builder()
                .status(status)
                .reason("error")
                .request(request)
                .body(body, StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("methodKey", response);
    }
}
