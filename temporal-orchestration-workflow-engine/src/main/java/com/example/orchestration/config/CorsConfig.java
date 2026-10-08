package com.example.orchestration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the browser-based Workflow Editor (a different origin/port) to call
 * {@code /api/workflow-definitions} directly. Server-to-server calls (e.g. account-creation-
 * submission calling {@code /api/orchestration/**}) are unaffected by CORS - this is purely a
 * browser requirement.
 * <p>
 * This service has no Spring Security filter chain (unlike create-account's CorsConfig, which
 * feeds a {@code CorsConfigurationSource} into {@code .cors(Customizer.withDefaults())}), so
 * plain Spring MVC CORS registration is enough - Spring's dispatcher applies it directly, no
 * earlier filter to route it through.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:5174}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
