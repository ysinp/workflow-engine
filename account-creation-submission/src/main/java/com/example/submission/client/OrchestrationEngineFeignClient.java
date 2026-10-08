package com.example.submission.client;

import com.example.submission.dto.EngineDecisionRequest;
import com.example.submission.dto.EngineStartRequest;
import com.example.submission.dto.EngineStartResponse;
import com.example.submission.dto.EngineStatusResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/** Declarative client for temporal-orchestration-workflow-engine; the service layer never sees Feign. */
@FeignClient(name = "orchestration-engine", url = "${services.orchestration-engine.base-url}")
public interface OrchestrationEngineFeignClient {

    @PostMapping("/api/orchestration/workflows/account-creation/executions")
    EngineStartResponse startAccountCreation(@RequestBody EngineStartRequest request);

    @GetMapping("/api/orchestration/executions/{workflowId}")
    EngineStatusResponse getStatus(@PathVariable("workflowId") String workflowId);

    @GetMapping("/api/orchestration/executions/awaiting-review")
    List<EngineStatusResponse> listAwaitingReview();

    @PostMapping("/api/orchestration/executions/{workflowId}/decision")
    void submitReviewDecision(@PathVariable("workflowId") String workflowId, @RequestBody EngineDecisionRequest request);
}
