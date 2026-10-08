package com.example.submission.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates this service's unchecked exceptions (and Bean Validation failures) into the
 * exact HTTP response shapes the API contract requires. This is the only place in the
 * service that knows about HTTP status codes; downstream failures are never leaked to the
 * client as raw stack traces.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", "Request body is invalid", fieldErrors));
    }

    @ExceptionHandler(SubmissionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(SubmissionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("SUBMISSION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ReviewerAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleReviewerAccessDenied(ReviewerAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("REVIEWER_ACCESS_REQUIRED", ex.getMessage()));
    }

    @ExceptionHandler(OrchestrationEngineException.class)
    public ResponseEntity<ErrorResponse> handleEngineFailure(OrchestrationEngineException ex) {
        log.error("Call to the orchestration engine failed", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("ORCHESTRATION_ENGINE_UNAVAILABLE",
                        "Failed to reach the orchestration engine"));
    }

    @ExceptionHandler(DuplicateApplicationException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateApplication(DuplicateApplicationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_EMAIL", ex.getMessage()));
    }

    @ExceptionHandler(AccountApplicationServiceException.class)
    public ResponseEntity<ErrorResponse> handleAccountApplicationFailure(AccountApplicationServiceException ex) {
        log.error("Call to create-account's application endpoint failed", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("ACCOUNTS_SERVICE_UNAVAILABLE",
                        "Failed to reach the accounts service"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
