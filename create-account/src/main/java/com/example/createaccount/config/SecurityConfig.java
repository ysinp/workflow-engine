package com.example.createaccount.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT security.
 * <ul>
 *   <li>Public: {@code POST /api/accounts} (signup), {@code POST /api/accounts/signin},
 *       {@code GET /api/accounts/exists} (called service-to-service, unauthenticated).</li>
 *   <li>Public: everything under {@code /api/account-applications/**} - {@code POST}/{@code GET}
 *       are called (indirectly) by the frontend, {@code approve}/{@code reject} are called
 *       service-to-service by the orchestration engine, same convention as {@code /exists}.</li>
 *   <li>{@code GET /api/accounts} and {@code GET /api/accounts/{id}} require a valid bearer JWT
 *       carrying the REVIEWER role - this is the accounts dashboard, which reads back every
 *       customer's KYC profile.</li>
 *   <li>Everything else under {@code /api/**} requires a valid bearer JWT.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   AuthenticationEntryPoint authenticationEntryPoint) throws Exception {
        http
                // Stateless bearer-token API with no cookies/sessions: CSRF protection is not applicable.
                .csrf(AbstractHttpConfigurer::disable)
                // Uses the "corsConfigurationSource" bean from CorsConfig.
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/accounts", "/api/accounts/signin").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/accounts/exists").permitAll()
                        .requestMatchers("/api/account-applications/**").permitAll()
                        // The accounts list/detail reads back every customer's KYC profile - that's
                        // reviewer/staff data, not something every signed-in customer should see.
                        .requestMatchers(HttpMethod.GET, "/api/accounts", "/api/accounts/*").hasAuthority("REVIEWER")
                        // Prometheus scrapes /actuator/prometheus unauthenticated (see observability/);
                        // the set of exposed actuator endpoints is already restricted in application.yml.
                        .requestMatchers("/actuator/**").permitAll()
                        // Spring Boot forwards errors (e.g. malformed JSON on a public endpoint) to /error.
                        // Without this, that ERROR dispatch would itself be rejected and turn a 400 into a 401.
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return new JsonAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
