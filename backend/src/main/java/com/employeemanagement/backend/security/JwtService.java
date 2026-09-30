package com.employeemanagement.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String SESSION_EXPIRES_AT_CLAIM = "session_expires_at";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final String jwtSecret;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.jwtSecret = jwtSecret;
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(userDetails.getUsername(), createSessionExpiresAt());
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateRefreshToken(userDetails.getUsername(), createSessionExpiresAt());
    }

    public String generateAccessToken(String username, long sessionExpiresAt) {
        return generateToken(username, ACCESS_TOKEN_TYPE, sessionExpiresAt, accessExpirationMs);
    }

    public String generateRefreshToken(String username, long sessionExpiresAt) {
        return generateToken(username, REFRESH_TOKEN_TYPE, sessionExpiresAt, refreshExpirationMs);
    }

    private String generateToken(String username, String tokenType, long sessionExpiresAt, long maxExpirationMs) {
        long now = System.currentTimeMillis();
        long remainingSessionMs = sessionExpiresAt - now;

        if (remainingSessionMs <= 0) {
            throw new IllegalStateException("Session has expired");
        }

        return Jwts.builder()
                .subject(username)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .claim(SESSION_EXPIRES_AT_CLAIM, sessionExpiresAt)
                .issuedAt(new Date(now))
                .expiration(new Date(now + Math.min(maxExpirationMs, remainingSessionMs)))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails.getUsername(), ACCESS_TOKEN_TYPE);
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                    && isSessionStillActive(claims)
                    && !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public long extractSessionExpiresAt(String token) {
        return extractAllClaims(token).get(SESSION_EXPIRES_AT_CLAIM, Long.class);
    }

    private boolean isTokenValid(String token, String username, String tokenType) {
        try {
            Claims claims = extractAllClaims(token);
            return username.equals(claims.getSubject())
                    && tokenType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                    && isSessionStillActive(claims)
                    && !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isSessionStillActive(Claims claims) {
        Long sessionExpiresAt = claims.get(SESSION_EXPIRES_AT_CLAIM, Long.class);
        return sessionExpiresAt != null && System.currentTimeMillis() < sessionExpiresAt;
    }

    public long createSessionExpiresAt() {
        return System.currentTimeMillis() + refreshExpirationMs;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured using app.jwt.secret");
        }

        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
