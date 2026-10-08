package com.example.orchestration.activity;

import com.example.orchestration.service.PendingActivityTokenRegistry;
import com.example.orchestration.workflow.ReviewDecision;
import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import org.springframework.stereotype.Component;

/**
 * Every method parks: it registers its own task token under (workflowId, step name) and never
 * completes on return. The actual step data (identificationId, risk assessment, etc.) already
 * lives in the workflow's own fields by the time one of these completes - set there by the
 * matching {@code @UpdateMethod}, which runs (and does the real I/O) before the REST layer
 * completes this gate. See {@link StepGateActivities} for why this exists at all.
 */
@Component
public class StepGateActivitiesImpl implements StepGateActivities {

    private final PendingActivityTokenRegistry registry;

    public StepGateActivitiesImpl(PendingActivityTokenRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void awaitProductSelection() {
        park("product");
    }

    @Override
    public void awaitPersonalContact() {
        park("personal");
    }

    @Override
    public void awaitIdentification() {
        park("identification");
    }

    @Override
    public void awaitFinancial() {
        park("financial");
    }

    @Override
    public void awaitSecurity() {
        park("security");
    }

    @Override
    public ReviewDecision awaitReviewDecision() {
        park("review");
        return null; // never actually returned - see the javadoc on StepGateActivities
    }

    private void park(String step) {
        ActivityExecutionContext ctx = Activity.getExecutionContext();
        String workflowId = ctx.getInfo().getWorkflowId();
        String temporalActivityId = ctx.getInfo().getActivityId();
        registry.register(workflowId, step, ctx.getTaskToken(), temporalActivityId);
        ctx.doNotCompleteOnReturn();
    }
}
