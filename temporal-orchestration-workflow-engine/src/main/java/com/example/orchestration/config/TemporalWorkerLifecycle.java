package com.example.orchestration.config;

import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Starts the Temporal worker once all singletons are created (after the context refresh) and
 * shuts it down gracefully - before bean destruction - when the context closes.
 */
@Component
public class TemporalWorkerLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TemporalWorkerLifecycle.class);
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final WorkerFactory workerFactory;
    private volatile boolean running;

    public TemporalWorkerLifecycle(WorkerFactory workerFactory) {
        this.workerFactory = workerFactory;
    }

    @Override
    public void start() {
        workerFactory.start();
        running = true;
        log.info("Temporal worker started, polling task queue '{}'", TemporalConstants.TASK_QUEUE);
    }

    @Override
    public void stop() {
        log.info("Shutting down Temporal worker");
        workerFactory.shutdown();
        workerFactory.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
