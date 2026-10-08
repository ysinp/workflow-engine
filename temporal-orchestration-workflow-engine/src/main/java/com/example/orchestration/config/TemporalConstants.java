package com.example.orchestration.config;

public final class TemporalConstants {

    /** Task queue polled by this service's worker and targeted by workflow starts. */
    public static final String TASK_QUEUE = "account-creation-tq";

    private TemporalConstants() {
    }
}
