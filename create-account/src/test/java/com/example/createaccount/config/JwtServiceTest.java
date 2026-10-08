package com.example.createaccount.config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-comfortably-longer-than-32-bytes";
    /** Must match the default in application.yml / JwtService config. */
    private static final String DEFAULT_SECRET =
            "dev-only-insecure-secret-change-me-please-this-needs-to-be-at-least-32-bytes-long";

    private final JwtService jwtService = new JwtService(SECRET, 4);

    @Test
    void generateThenParse_roundTripsTheSubjectAndRole() {
        String token = jwtService.generateToken("jane@example.com", "REVIEWER");

        assertThat(token.split("\\.")).hasSize(3);
        assertThat(jwtService.parseSubject(token)).isEqualTo("jane@example.com");
        assertThat(jwtService.parseRole(token)).isEqualTo("REVIEWER");
    }

    @Test
    void parse_tamperedPayload_throws() {
        String token = jwtService.generateToken("jane@example.com", "CUSTOMER");
        String[] parts = token.split("\\.");
        // Swap in a different (validly encoded) payload while keeping the original signature.
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"attacker@example.com\"}".getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> jwtService.parseSubject(tampered))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void parse_tokenSignedWithDifferentKey_throws() {
        JwtService other = new JwtService("a-completely-different-secret-also-over-32-bytes-long", 4);
        String foreignToken = other.generateToken("jane@example.com", "CUSTOMER");

        assertThatThrownBy(() -> jwtService.parseSubject(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parse_expiredToken_throws() {
        // Build an already-expired token with the same key (JwtService itself only issues future expirations).
        String expired = Jwts.builder()
                .subject("jane@example.com")
                .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.parseSubject(expired))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parse_garbage_throws() {
        assertThatThrownBy(() -> jwtService.parseSubject("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void defaultDevSecret_isAtLeast32Bytes_andIsAcceptedByKeys() {
        assertThat(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8).length).isGreaterThanOrEqualTo(32);
        String token = new JwtService(DEFAULT_SECRET, 4).generateToken("jane@example.com", "CUSTOMER");
        assertThat(new JwtService(DEFAULT_SECRET, 4).parseSubject(token)).isEqualTo("jane@example.com");
    }

    @Test
    void secretShorterThan32Bytes_isRejectedAtConstruction() {
        assertThatThrownBy(() -> new JwtService("too-short", 4))
                .isInstanceOf(WeakKeyException.class);
    }
}
