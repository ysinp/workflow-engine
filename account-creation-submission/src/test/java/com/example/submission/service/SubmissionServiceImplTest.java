package com.example.submission.service;

import com.example.submission.dto.AccountApplicationAcceptedResponse;
import com.example.submission.dto.AccountApplicationStartRequest;
import com.example.submission.dto.EngineDecisionRequest;
import com.example.submission.dto.EngineStartRequest;
import com.example.submission.dto.EngineStartResponse;
import com.example.submission.dto.EngineStatusResponse;
import com.example.submission.dto.RiskAssessmentDto;
import com.example.submission.dto.SubmissionAcceptedResponse;
import com.example.submission.dto.SubmissionResult;
import com.example.submission.dto.SubmissionStatusResponse;
import com.example.submission.dto.SubmitAccountRequest;
import com.example.submission.dto.SubmitReviewDecisionRequest;
import com.example.submission.exception.SubmissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock
    private AccountApplicationClient accountApplicationClient;

    @Mock
    private OrchestrationEngineClient orchestrationEngineClient;

    private SubmissionService newService() {
        return new SubmissionServiceImpl(accountApplicationClient, orchestrationEngineClient);
    }

    private static SubmitAccountRequest sampleRequest() {
        return new SubmitAccountRequest(
                "Jane Doe", "jane@example.com", "correct-horse-battery", "1990-01-01", "US", "555-0100",
                "1 Main St", null, "Springfield", "IL", "62701", "US",
                "PASSPORT", "A1234567", "Engineer", "50K_TO_100K", "EMPLOYMENT");
    }

    @Test
    void submit_stagesApplicationFirst_thenStartsEngineWorkflowWithItsApplicationId() {
        SubmissionService submissionService = newService();
        when(accountApplicationClient.submit(any(AccountApplicationStartRequest.class)))
                .thenReturn(new AccountApplicationAcceptedResponse("app-1", "PENDING_REVIEW", null));
        when(orchestrationEngineClient.startAccountCreation(any(EngineStartRequest.class)))
                .thenReturn(new EngineStartResponse("wf-123", "run-456", "RUNNING"));

        SubmissionAcceptedResponse response = submissionService.submit(sampleRequest());

        ArgumentCaptor<AccountApplicationStartRequest> applicationCaptor =
                ArgumentCaptor.forClass(AccountApplicationStartRequest.class);
        verify(accountApplicationClient).submit(applicationCaptor.capture());
        assertThat(applicationCaptor.getValue().fullName()).isEqualTo("Jane Doe");
        assertThat(applicationCaptor.getValue().email()).isEqualTo("jane@example.com");
        assertThat(applicationCaptor.getValue().password()).isEqualTo("correct-horse-battery");

        ArgumentCaptor<EngineStartRequest> engineCaptor = ArgumentCaptor.forClass(EngineStartRequest.class);
        verify(orchestrationEngineClient).startAccountCreation(engineCaptor.capture());

        EngineStartRequest sentToEngine = engineCaptor.getValue();
        assertThat(sentToEngine.applicationId()).isEqualTo("app-1");
        assertThat(sentToEngine.fullName()).isEqualTo("Jane Doe");
        assertThat(sentToEngine.email()).isEqualTo("jane@example.com");
        assertThat(sentToEngine.nationality()).isEqualTo("US");
        assertThat(sentToEngine.idNumber()).isEqualTo("A1234567");

        assertThat(response.submissionId()).isEqualTo("wf-123");
        assertThat(response.status()).isEqualTo("RUNNING");
    }

    @Test
    void getStatus_mapsEngineWorkflowIdToSubmissionId_whenRunning() {
        SubmissionService submissionService = newService();
        when(orchestrationEngineClient.getStatus("wf-123"))
                .thenReturn(new EngineStatusResponse("wf-123", "RUNNING", null, null, null));

        SubmissionStatusResponse response = submissionService.getStatus("wf-123");

        assertThat(response.submissionId()).isEqualTo("wf-123");
        assertThat(response.status()).isEqualTo("RUNNING");
        assertThat(response.result()).isNull();
        assertThat(response.errorMessage()).isNull();
        assertThat(response.pendingRiskAssessment()).isNull();
    }

    @Test
    void getStatus_mapsPendingRiskAssessment_whenAwaitingReview() {
        SubmissionService submissionService = newService();
        RiskAssessmentDto pending = new RiskAssessmentDto("HIGH", 100, List.of("reason"));
        when(orchestrationEngineClient.getStatus("wf-123"))
                .thenReturn(new EngineStatusResponse("wf-123", "AWAITING_REVIEW", null, null, pending));

        SubmissionStatusResponse response = submissionService.getStatus("wf-123");

        assertThat(response.status()).isEqualTo("AWAITING_REVIEW");
        assertThat(response.pendingRiskAssessment()).isEqualTo(pending);
    }

    @Test
    void getStatus_mapsEngineResult_whenCompleted() {
        SubmissionService submissionService = newService();
        SubmissionResult result = new SubmissionResult("APPROVED", "LOW", 0, List.of());
        when(orchestrationEngineClient.getStatus("wf-123"))
                .thenReturn(new EngineStatusResponse("wf-123", "COMPLETED", result, null, null));

        SubmissionStatusResponse response = submissionService.getStatus("wf-123");

        assertThat(response.submissionId()).isEqualTo("wf-123");
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.result()).isEqualTo(result);
        assertThat(response.errorMessage()).isNull();
    }

    @Test
    void getStatus_mapsEngineErrorMessage_whenFailed() {
        SubmissionService submissionService = newService();
        when(orchestrationEngineClient.getStatus("wf-123"))
                .thenReturn(new EngineStatusResponse("wf-123", "FAILED", null, "risk-score service unreachable", null));

        SubmissionStatusResponse response = submissionService.getStatus("wf-123");

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.errorMessage()).isEqualTo("risk-score service unreachable");
    }

    @Test
    void getStatus_propagatesNotFound_whenEngineReports404() {
        SubmissionService submissionService = newService();
        when(orchestrationEngineClient.getStatus(eq("unknown-id")))
                .thenThrow(new SubmissionNotFoundException("unknown-id"));

        assertThatThrownBy(() -> submissionService.getStatus("unknown-id"))
                .isInstanceOf(SubmissionNotFoundException.class);
    }

    @Test
    void submitReviewDecision_forwardsDecisionToEngine() {
        SubmissionService submissionService = newService();
        SubmitReviewDecisionRequest request = new SubmitReviewDecisionRequest("APPROVE", "reviewer-1", "looks fine");

        submissionService.submitReviewDecision("wf-123", request);

        ArgumentCaptor<EngineDecisionRequest> captor = ArgumentCaptor.forClass(EngineDecisionRequest.class);
        verify(orchestrationEngineClient).submitReviewDecision(eq("wf-123"), captor.capture());
        assertThat(captor.getValue().decision()).isEqualTo("APPROVE");
        assertThat(captor.getValue().reviewerId()).isEqualTo("reviewer-1");
        assertThat(captor.getValue().notes()).isEqualTo("looks fine");
    }

    @Test
    void listAwaitingReview_mapsEachEngineEntryToASubmissionStatusResponse() {
        SubmissionService submissionService = newService();
        RiskAssessmentDto pending = new RiskAssessmentDto("HIGH", 100, List.of("reason"));
        when(orchestrationEngineClient.listAwaitingReview())
                .thenReturn(List.of(new EngineStatusResponse("wf-123", "AWAITING_REVIEW", null, null, pending)));

        List<SubmissionStatusResponse> responses = submissionService.listAwaitingReview();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).submissionId()).isEqualTo("wf-123");
        assertThat(responses.get(0).status()).isEqualTo("AWAITING_REVIEW");
        assertThat(responses.get(0).pendingRiskAssessment()).isEqualTo(pending);
    }
}
