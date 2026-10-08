package com.example.createaccount.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Issues and verifies HMAC-signed JWTs (JJWT 0.13.x API).
 * <p>
 * Note: {@code signWith(SecretKey)} lets JJWT pick the strongest HMAC algorithm the key length
 * allows (HS256 for 32-47 bytes, HS384 for 48-63, HS512 for 64+). The verifier uses the same key,
 * so this is transparent to callers.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationHours;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-hours}") long expirationHours) {
        // Keys.hmacShaKeyFor throws WeakKeyException if the secret is < 32 bytes (256 bits),
        // which fails startup fast rather than silently issuing weak tokens.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(expirationHours, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies the signature and expiration and returns the subject (email).
     *
     * @throws JwtException (e.g. ExpiredJwtException, SignatureException, MalformedJwtException)
     *         if the token is invalid or expired; IllegalArgumentException if null/blank
     */
    public String parseSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /** Same validity rules as {@link #parseSubject(String)}; returns the "role" claim, or null. */
    public String parseRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
