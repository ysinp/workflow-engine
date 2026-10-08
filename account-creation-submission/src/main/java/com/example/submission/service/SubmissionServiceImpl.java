package com.example.submission.service;

import com.example.submission.dto.AccountApplicationAcceptedResponse;
import com.example.submission.dto.AccountApplicationStartRequest;
import com.example.submission.dto.EngineDecisionRequest;
import com.example.submission.dto.EngineStartRequest;
import com.example.submission.dto.EngineStartResponse;
import com.example.submission.dto.EngineStatusResponse;
import com.example.submission.dto.SubmissionAcceptedResponse;
import com.example.submission.dto.SubmissionStatusResponse;
import com.example.submission.dto.SubmitAccountRequest;
import com.example.submission.dto.SubmitReviewDecisionRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The single entry point for a KYC signup. {@code submit} first stages the application in
 * create-account (password BCrypt-hashed there, never touching Temporal), then starts the
 * risk-assessment workflow that will activate it (create the real Account row) or reject it.
 */
@Service
public class SubmissionServiceImpl implements SubmissionService {

    private final AccountApplicationClient accountApplicationClient;
    private final OrchestrationEngineClient orchestrationEngineClient;

    public SubmissionServiceImpl(AccountApplicationClient accountApplicationClient,
                                 OrchestrationEngineClient orchestrationEngineClient) {
        this.accountApplicationClient = accountApplicationClient;
        this.orchestrationEngineClient = orchestrationEngineClient;
    }

    @Override
    public SubmissionAcceptedResponse submit(SubmitAccountRequest request) {
        AccountApplicationAcceptedResponse application = accountApplicationClient.submit(
                new AccountApplicationStartRequest(
                        request.fullName(), request.email(), request.password(), request.dateOfBirth(),
                        request.nationality(), request.phone(), request.addressLine1(), request.addressLine2(),
                        request.city(), request.state(), request.postalCode(), request.country(),
                        request.idType(), request.idNumber(), request.occupation(), request.annualIncome(),
                        request.sourceOfFunds()));

        EngineStartResponse started = orchestrationEngineClient.startAccountCreation(new EngineStartRequest(
                application.applicationId(),
                request.fullName(), request.email(), request.dateOfBirth(), request.nationality(),
                request.phone(), request.addressLine1(), request.addressLine2(), request.city(),
                request.state(), request.postalCode(), request.country(), request.idType(),
                request.idNumber(), request.occupation(), request.annualIncome(), request.sourceOfFunds()));
        return new SubmissionAcceptedResponse(started.workflowId(), started.status());
    }

    @Override
    public SubmissionStatusResponse getStatus(String submissionId) {
        EngineStatusResponse status = orchestrationEngineClient.getStatus(submissionId);
        return new SubmissionStatusResponse(
                status.workflowId(),
                status.status(),
                status.result(),
                status.errorMessage(),
                status.pendingRiskAssessment());
    }

    @Override
    public void submitReviewDecision(String submissionId, SubmitReviewDecisionRequest request) {
        orchestrationEngineClient.submitReviewDecision(submissionId, EngineDecisionRequest.from(request));
    }

    @Override
    public List<SubmissionStatusResponse> listAwaitingReview() {
        return orchestrationEngineClient.listAwaitingReview().stream()
                .map(status -> new SubmissionStatusResponse(
                        status.workflowId(),
                        status.status(),
                        status.result(),
                        status.errorMessage(),
                        status.pendingRiskAssessment()))
                .toList();
    }
}
