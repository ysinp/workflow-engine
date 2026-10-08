package com.example.orchestration.activity;

import com.example.orchestration.workflow.ReviewDecision;
import io.temporal.activity.ActivityInterface;

/**
 * One activity per wizard step, Product Selection through the compliance reviewer's decision.
 * Each is scheduled normally by the workflow, but its implementation ({@code StepGateActivitiesImpl})
 * never completes on return - it parks (see {@code Activity.doNotCompleteOnReturn()}) until a human
 * acts in the browser and the REST layer completes it externally via an
 * {@code io.temporal.client.ActivityCompletionClient}, keyed by this workflow's ID and the step
 * name. This is what makes each step a genuine, independently visible (Temporal UI "Pending
 * Activities") unit of human-in-the-loop work, rather than an Update that resolves on its own.
 */
@ActivityInterface
public interface StepGateActivities {

    void awaitProductSelection();

    void awaitPersonalContact();

    void awaitIdentification();

    void awaitFinancial();

    void awaitSecurity();

    /**
     * The return value here is never actually used - the method never returns normally (it
     * parks). The real {@link ReviewDecision} is supplied by whoever completes this activity
     * externally, and that's the value {@link #awaitReviewDecision} appears to return to the
     * workflow once Temporal delivers the completion.
     */
    ReviewDecision awaitReviewDecision();
}
