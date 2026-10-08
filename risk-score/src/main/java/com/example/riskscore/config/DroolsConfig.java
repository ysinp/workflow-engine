package com.example.riskscore.config;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Compiles {@code src/main/resources/rules/risk-rules.drl} (discovered via
 * {@code META-INF/kmodule.xml}) into a {@link KieContainer} at startup.
 *
 * <p>The container itself is stateless and safe to share as a singleton; {@code
 * RiskScoringServiceImpl} creates and disposes a fresh {@code KieSession} from it per request.
 */
@Configuration
public class DroolsConfig {

    @Bean
    public KieContainer kieContainer() {
        return KieServices.Factory.get().getKieClasspathContainer();
    }
}
