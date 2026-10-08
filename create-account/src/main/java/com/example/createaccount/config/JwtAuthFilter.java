package com.example.createaccount.config;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates requests carrying {@code Authorization: Bearer <jwt>}.
 * <ul>
 *   <li>No header (or not a Bearer header): continue unauthenticated.</li>
 *   <li>Valid token: set an authenticated {@link UsernamePasswordAuthenticationToken}
 *       (principal = email from the token subject, no authorities).</li>
 *   <li>Invalid/expired token: do NOT throw — continue unauthenticated, so a bad token behaves
 *       exactly like no token (protected endpoints then get the normal JSON 401, not a 500).</li>
 * </ul>
 * Intentionally NOT a {@code @Component}: Spring Boot auto-registers every Filter bean as a
 * servlet filter, which would run it outside the security chain as well. It is instantiated by
 * {@link SecurityConfig} and added only to the security filter chain.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                String email = jwtService.parseSubject(token);
                if (email != null && !email.isBlank()) {
                    String role = jwtService.parseRole(token);
                    List<GrantedAuthority> authorities =
                            role != null && !role.isBlank() ? List.of(new SimpleGrantedAuthority(role)) : List.of();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authentication);
                    SecurityContextHolder.setContext(context);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // Invalid, tampered, expired, or empty token: treat as unauthenticated.
                log.debug("Rejected bearer token: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
