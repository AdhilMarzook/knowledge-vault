package com.knowledgevault.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT lifecycle management.
 * Uses HMAC-SHA512 (HS512) — the strongest symmetric JJWT option.
 *
 * Access  tokens  — short-lived (15 min by default)
 * Refresh tokens  — longer-lived (7 days by default), stored server-side for revocation
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    // In-memory revocation set (use Redis in production for distributed revocation)
    private final java.util.Set<String> revokedJtis =
        java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public JwtService(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token.expiry-ms:900000}") long accessTokenExpiryMs,
        @Value("${jwt.refresh-token.expiry-ms:604800000}") long refreshTokenExpiryMs
    ) {
        // Derive a HS512-safe key from the configured secret
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // Ensure minimum key length for HS512 (64 bytes)
        if (keyBytes.length < 64) {
            throw new IllegalStateException(
                "JWT secret must be at least 64 characters for HS512. Use: openssl rand -hex 64"
            );
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    // ── Token Generation ──────────────────────────────────────────────────────

    public String generateAccessToken(String userId, String username, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
            .id(UUID.randomUUID().toString())                       // jti — unique per token
            .subject(userId)
            .claim("username", username)
            .claim("roles", roles)
            .claim("type", "access")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(accessTokenExpiryMs)))
            .signWith(signingKey, Jwts.SIG.HS512)
            .compact();
    }

    public String generateRefreshToken(String userId) {
        Instant now = Instant.now();
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(userId)
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(refreshTokenExpiryMs)))
            .signWith(signingKey, Jwts.SIG.HS512)
            .compact();
    }

    // ── Token Parsing ─────────────────────────────────────────────────────────

    public Claims parseAndValidate(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        // Check revocation
        String jti = claims.getId();
        if (jti != null && revokedJtis.contains(jti)) {
            throw new JwtException("Token has been revoked");
        }

        return claims;
    }

    public String extractUserId(String token) {
        return parseAndValidate(token).getSubject();
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }

    /** Validate silently — returns false instead of throwing */
    public boolean isValid(String token) {
        try {
            parseAndValidate(token);
            return true;
        } catch (SignatureException e) {
            log.warn("JWT signature invalid");
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
        }
        return false;
    }

    /** Revoke a token by its jti (logout / refresh rotation) */
    public void revoke(String token) {
        try {
            Claims claims = parseAndValidate(token);
            String jti = claims.getId();
            if (jti != null) {
                revokedJtis.add(jti);
                // TODO (production): persist jti + expiry in Redis with TTL matching token expiry
            }
        } catch (JwtException ignored) {
            // Already invalid — nothing to revoke
        }
    }

    public long getAccessTokenExpiryMs() { return accessTokenExpiryMs; }
}
