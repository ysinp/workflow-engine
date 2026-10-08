package com.example.orchestration.workflow;

/**
 * A compliance reviewer's decision on a HIGH-risk application, delivered via
 * {@link AccountCreationWorkflow#submitReviewDecision}.
 *
 * @param decision   {@link #APPROVE} or {@link #REJECT}
 * @param reviewerId free-text identifier of who made the call (a name/email is fine for this POC)
 * @param notes      optional reviewer notes, included in the rejection/approval notification
 */
public record ReviewDecision(String decision, String reviewerId, String notes) {

    public static final String APPROVE = "APPROVE";
    public static final String REJECT = "REJECT";

    // Deliberately NOT an isApprove() method: Jackson's default JavaBean property detection
    // treats any "isXxx()" instance method as a getter for property "xxx", which would add a
    // phantom "approve" field to this record's serialized JSON. Since this type crosses the wire
    // as a Temporal signal payload, that extra field then fails deserialization on the receiving
    // side with an UnrecognizedPropertyException - and Temporal's SignalDispatcher logs that and
    // silently drops the signal rather than failing loudly, so the workflow just hangs forever
    // waiting for a signal that never actually arrives. Cost several hours to track down; use a
    // plain static method instead so Jackson has nothing to misdetect.
    public static boolean isApprove(ReviewDecision decision) {
        return APPROVE.equals(decision.decision());
    }
}
