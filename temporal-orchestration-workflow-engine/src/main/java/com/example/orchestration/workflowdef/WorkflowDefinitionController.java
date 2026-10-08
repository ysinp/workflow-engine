package com.example.orchestration.workflowdef;

import com.example.orchestration.service.ReviewerAuthGuard;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Persists Workflow Editor diagrams (the SDK's exported JSON) so they survive across browsers and
 * reloads, instead of living only in the editor's own localStorage. Reviewer-gated, matching the
 * {@code /workflow-editor} frontend route's own access control.
 */
@RestController
@RequestMapping("/api/workflow-definitions")
public class WorkflowDefinitionController {

    private final WorkflowDefinitionService definitions;
    private final ReviewerAuthGuard reviewerAuthGuard;

    public WorkflowDefinitionController(WorkflowDefinitionService definitions, ReviewerAuthGuard reviewerAuthGuard) {
        this.definitions = definitions;
        this.reviewerAuthGuard = reviewerAuthGuard;
    }

    @PostMapping
    public WorkflowDefinitionResponse save(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SaveWorkflowDefinitionRequest request) {
        String reviewerEmail = reviewerAuthGuard.requireReviewer(authorization);
        var saved = definitions.save(request.name(), request.definition(), reviewerEmail);
        return toResponse(saved);
    }

    @GetMapping("/{name}")
    public WorkflowDefinitionResponse getByName(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable String name) {
        reviewerAuthGuard.requireReviewer(authorization);
        return toResponse(definitions.getByName(name));
    }

    @GetMapping
    public List<WorkflowDefinitionSummary> list(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        reviewerAuthGuard.requireReviewer(authorization);
        return definitions.listAll().stream().map(WorkflowDefinitionSummary::from).toList();
    }

    private WorkflowDefinitionResponse toResponse(WorkflowDefinition definition) {
        return new WorkflowDefinitionResponse(
                definition.getId(),
                definition.getName(),
                definitions.readDefinition(definition),
                definition.getUpdatedAt(),
                definition.getUpdatedBy());
    }
}
