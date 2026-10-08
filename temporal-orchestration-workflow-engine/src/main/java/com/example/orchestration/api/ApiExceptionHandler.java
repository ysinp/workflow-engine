package com.example.orchestration.api;

import com.example.orchestration.service.ExecutionNotFoundException;
import com.example.orchestration.service.ReviewerAccessDeniedException;
import com.example.orchestration.workflow.OnboardingWizardWorkflowImpl;
import com.example.orchestration.workflowdef.WorkflowDefinitionNotFoundException;
import io.temporal.client.WorkflowException;
import io.temporal.client.WorkflowUpdateException;
import io.temporal.failure.ApplicationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Returns RFC 7807 {@link ProblemDetail} JSON for every error. Standard Spring MVC exceptions
 * (400 malformed JSON, 405, 404 unknown path, ...) are handled by the base class.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ExecutionNotFoundException.class)
    public ProblemDetail handleNotFound(ExecutionNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Workflow execution not found", e.getMessage());
    }

    @ExceptionHandler(WorkflowDefinitionNotFoundException.class)
    public ProblemDetail handleDefinitionNotFound(WorkflowDefinitionNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Workflow definition not found", e.getMessage());
    }

    @ExceptionHandler(ReviewerAccessDeniedException.class)
    public ProblemDetail handleReviewerAccessDenied(ReviewerAccessDeniedException e) {
        return problem(HttpStatus.FORBIDDEN, "Access denied", e.getMessage());
    }

    /**
     * A step Update rejected because the workflow has already moved past it (see
     * {@code OnboardingWizardWorkflowImpl#requireCurrentStep}) - the frontend's own navigation
     * should already prevent this; reaching here means something resubmitted a completed step
     * anyway (e.g. a stale tab or the browser's back button).
     */
    @ExceptionHandler(WorkflowUpdateException.class)
    public ProblemDetail handleUpdateFailure(WorkflowUpdateException e) {
        if (isStepOrderViolation(e)) {
            return problem(HttpStatus.CONFLICT, "Step already completed",
                    "This step has already been completed and cannot be resubmitted.");
        }
        log.error("Workflow update failed", e);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Workflow engine unavailable",
                "Call to Temporal failed: " + e.getMessage());
    }

    /** Temporal client-side failures (e.g. server unreachable, RPC error). */
    @ExceptionHandler(WorkflowException.class)
    public ProblemDetail handleTemporal(WorkflowException e) {
        log.error("Temporal call failed", e);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Workflow engine unavailable",
                "Call to Temporal failed: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error("Unhandled error", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "An unexpected error occurred");
    }

    /** Adds a {@code fieldErrors} map to the standard 400 validation problem. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validation failed", "Request body is invalid");
        pd.setProperty("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(pd);
    }

    private static boolean isStepOrderViolation(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof ApplicationFailure af
                    && OnboardingWizardWorkflowImpl.STEP_ORDER_VIOLATION_TYPE.equals(af.getType())) {
                return true;
            }
        }
        return false;
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        return pd;
    }
}
