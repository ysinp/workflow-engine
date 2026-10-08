package com.example.createaccount.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Allows the browser-based frontend dashboard (a different origin/port) to call this API
 * directly. curl/server-to-server calls (e.g. from the orchestration engine's activity) are
 * unaffected by CORS - this is purely a browser requirement.
 * <p>
 * Exposed as a {@link CorsConfigurationSource} bean named {@code corsConfigurationSource} (rather
 * than the previous {@code WebMvcConfigurer}) so that Spring Security's
 * {@code .cors(Customizer.withDefaults())} picks it up explicitly. The security filter chain
 * runs BEFORE Spring MVC, so CORS must be resolved there — otherwise a preflight {@code OPTIONS}
 * request (which never carries the {@code Authorization} header) could be rejected with a 401
 * before MVC's CORS handling ever sees it. This bean is the single source of truth for CORS;
 * there is no second MVC-level copy of the origin list.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:5174}")
    private String[] allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // "*" covers the Authorization header used for "Bearer <jwt>".
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
