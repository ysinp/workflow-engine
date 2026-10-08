package com.example.submission.service;

import com.example.submission.client.OrchestrationEngineFeignClient;
import com.example.submission.dto.EngineDecisionRequest;
import com.example.submission.dto.EngineStartRequest;
import com.example.submission.dto.EngineStartResponse;
import com.example.submission.dto.EngineStatusResponse;
import com.example.submission.exception.OrchestrationEngineException;
import com.example.submission.exception.SubmissionNotFoundException;
import feign.FeignException;
import org.springframework.stereotype.Component;

import java.util.List;

/** All I/O with the orchestration engine lives here; the service layer never sees Feign. */
@Component
public class FeignOrchestrationEngineClient implements OrchestrationEngineClient {

    private final OrchestrationEngineFeignClient client;

    public FeignOrchestrationEngineClient(OrchestrationEngineFeignClient client) {
        this.client = client;
    }

    @Override
    public EngineStartResponse startAccountCreation(EngineStartRequest request) {
        try {
            return client.startAccountCreation(request);
        } catch (FeignException e) {
            throw new OrchestrationEngineException(
                    "Orchestration engine start-execution call failed with HTTP " + e.status(), e);
        }
    }

    @Override
    public EngineStatusResponse getStatus(String workflowId) {
        try {
            return client.getStatus(workflowId);
        } catch (FeignException.NotFound e) {
            throw new SubmissionNotFoundException(workflowId);
        } catch (FeignException e) {
            throw new OrchestrationEngineException(
                    "Orchestration engine status call failed with HTTP " + e.status(), e);
        }
    }

    @Override
    public List<EngineStatusResponse> listAwaitingReview() {
        try {
            List<EngineStatusResponse> response = client.listAwaitingReview();
            return response != null ? response : List.of();
        } catch (FeignException e) {
            throw new OrchestrationEngineException(
                    "Orchestration engine awaiting-review call failed with HTTP " + e.status(), e);
        }
    }

    @Override
    public void submitReviewDecision(String workflowId, EngineDecisionRequest request) {
        try {
            client.submitReviewDecision(workflowId, request);
        } catch (FeignException.NotFound e) {
            throw new SubmissionNotFoundException(workflowId);
        } catch (FeignException e) {
            throw new OrchestrationEngineException(
                    "Orchestration engine decision call failed with HTTP " + e.status(), e);
        }
    }
}
