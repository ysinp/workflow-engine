package com.example.createaccount.config;

import com.example.createaccount.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Returns {@code 401 {"code":"UNAUTHORIZED","message":"..."}} for unauthenticated requests to
 * protected endpoints, instead of Spring Security's default (an empty 401/403 or a
 * WWW-Authenticate basic-auth challenge). This runs inside the security filter chain, before
 * any controller, so {@code GlobalExceptionHandler} cannot handle this case.
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(
                "UNAUTHORIZED",
                "Authentication required: provide a valid 'Authorization: Bearer <token>' header"));
    }
}
