package com.example.submission.service;

import com.example.submission.dto.EngineDecisionRequest;
import com.example.submission.dto.EngineStartRequest;
import com.example.submission.dto.EngineStartResponse;
import com.example.submission.dto.EngineStatusResponse;
import com.example.submission.exception.OrchestrationEngineException;
import com.example.submission.exception.SubmissionNotFoundException;

import java.util.List;

/**
 * DIP/SRP boundary: the only thing in this service that knows the orchestration engine is
 * reached over HTTP. The service layer talks to this interface, not to RestClient directly.
 */
public interface OrchestrationEngineClient {

    /**
     * @throws OrchestrationEngineException if the engine cannot be reached or responds
     *                                       unexpectedly
     */
    EngineStartResponse startAccountCreation(EngineStartRequest request);

    /**
     * @throws SubmissionNotFoundException  if the engine has no execution for this workflow id
     * @throws OrchestrationEngineException if the engine cannot be reached or responds
     *                                       unexpectedly
     */
    EngineStatusResponse getStatus(String workflowId);

    /**
     * @throws SubmissionNotFoundException  if the engine has no execution for this workflow id
     * @throws OrchestrationEngineException if the engine cannot be reached or responds
     *                                       unexpectedly
     */
    void submitReviewDecision(String workflowId, EngineDecisionRequest request);

    /**
     * Every submission currently paused for a compliance decision.
     *
     * @throws OrchestrationEngineException if the engine cannot be reached or responds
     *                                       unexpectedly
     */
    List<EngineStatusResponse> listAwaitingReview();
}
