package com.example.submission.service;

import com.example.submission.exception.ReviewerAccessDeniedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Verifies the {@code Authorization: Bearer <jwt>} header on the compliance-reviewer endpoints
 * (the decision endpoint and the pending-review queue) carries a token create-account issued for
 * a REVIEWER account. This service never issues tokens itself - it only ever checks the ones
 * create-account signed, using the same shared {@code jwt.secret}.
 */
@Component
public class ReviewerAuthGuard {

    private static final Logger log = LoggerFactory.getLogger(ReviewerAuthGuard.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String REVIEWER_ROLE = "REVIEWER";

    private final SecretKey key;

    public ReviewerAuthGuard(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @throws ReviewerAccessDeniedException if the header is missing/malformed, the token is
     *                                       invalid or expired, or its role claim isn't REVIEWER
     */
    public void requireReviewer(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ReviewerAccessDeniedException("Missing bearer token");
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String role = claims.get("role", String.class);
            if (!REVIEWER_ROLE.equals(role)) {
                throw new ReviewerAccessDeniedException("Token does not carry the REVIEWER role");
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected bearer token on a reviewer-only endpoint: {}", e.getMessage());
            throw new ReviewerAccessDeniedException("Invalid or expired token");
        }
    }
}
