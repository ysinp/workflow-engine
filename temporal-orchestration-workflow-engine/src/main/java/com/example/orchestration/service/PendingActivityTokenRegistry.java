package com.example.orchestration.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps (workflowId, step name) to the task token of that step's currently-parked gate activity
 * (see {@code StepGateActivitiesImpl}), so the REST layer can complete it by step name without
 * ever handling Temporal's own auto-assigned activity IDs or needing a database for this POC's
 * single-instance deployment. Also keeps that same gate's real Temporal activity ID alongside the
 * token - purely informational (never needed to complete it), surfaced in API responses so a
 * caller can correlate a step with its actual Temporal UI / Pending Activities entry.
 */
@Component
public class PendingActivityTokenRegistry {

    private static final long AWAIT_TIMEOUT_MS = 2000;
    private static final long POLL_INTERVAL_MS = 50;

    private final ConcurrentHashMap<String, PendingGate> gates = new ConcurrentHashMap<>();

    public void register(String workflowId, String step, byte[] taskToken, String temporalActivityId) {
        gates.put(key(workflowId, step), new PendingGate(taskToken, temporalActivityId));
    }

    /**
     * Removes and returns the gate for (workflowId, step) - use when actually completing it.
     * Waits briefly if it isn't registered yet, absorbing the one legitimate race in this design:
     * right after a workflow starts (or right after its previous gate completes), there's a brief
     * window before the worker has picked up and registered the *next* gate activity. Every other
     * step has ample time to clear this window on its own, since a human fills out a form in
     * between; only the very first ("product") gate can realistically be hit before it's registered.
     *
     * @return null if the gate never appeared within the timeout
     */
    public PendingGate take(String workflowId, String step) {
        return await(workflowId, step, true);
    }

    /**
     * Same wait as {@link #take}, but doesn't remove the entry - use to report the *next* step's
     * real Temporal activity ID in a response without consuming the gate that step itself will
     * later complete.
     *
     * @return null if the gate never appeared within the timeout
     */
    public PendingGate peek(String workflowId, String step) {
        return await(workflowId, step, false);
    }

    private PendingGate await(String workflowId, String step, boolean remove) {
        String k = key(workflowId, step);
        long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS;
        PendingGate gate;
        while ((gate = remove ? gates.remove(k) : gates.get(k)) == null && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        return gate;
    }

    private static String key(String workflowId, String step) {
        return workflowId + ":" + step;
    }

    public record PendingGate(byte[] taskToken, String temporalActivityId) {
    }
}
