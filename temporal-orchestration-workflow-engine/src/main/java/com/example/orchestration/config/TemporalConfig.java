package com.example.orchestration.config;

import com.example.orchestration.activity.AccountActivitiesImpl;
import com.example.orchestration.activity.OnboardingActivitiesImpl;
import com.example.orchestration.activity.StepGateActivitiesImpl;
import com.example.orchestration.workflow.OnboardingWizardWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Manual wiring of the Temporal core SDK (no spring-boot starter).
 *
 * <p>The {@link WorkerFactory} is only <em>built</em> here; starting/stopping it is owned by
 * {@link TemporalWorkerLifecycle} so polling begins after the context is fully initialised.
 */
@Configuration
public class TemporalConfig {

    @Bean(destroyMethod = "shutdown")
    public WorkflowServiceStubs workflowServiceStubs(@Value("${temporal.target:127.0.0.1:7233}") String target) {
        // newServiceStubs connects lazily, so the app can boot before Temporal is reachable.
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder().setTarget(target).build());
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        return WorkflowClient.newInstance(serviceStubs);
    }

    // destroyMethod = "": Spring would otherwise infer shutdown() as a destroy method; the
    // TemporalWorkerLifecycle bean is the single owner of start/stop.
    @Bean(destroyMethod = "")
    public WorkerFactory workerFactory(WorkflowClient workflowClient, AccountActivitiesImpl accountActivities,
                                        OnboardingActivitiesImpl onboardingActivities,
                                        StepGateActivitiesImpl stepGateActivities) {
        WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
        Worker worker = factory.newWorker(TemporalConstants.TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(OnboardingWizardWorkflowImpl.class);
        worker.registerActivitiesImplementations(accountActivities, onboardingActivities, stepGateActivities);
        return factory;
    }
}
