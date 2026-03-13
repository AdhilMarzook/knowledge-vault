package com.knowledgevault.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limiting using the token-bucket algorithm (Bucket4j).
 *
 * Limits:
 *  - Auth endpoints (/api/auth/**): 10 requests / 15 min  — brute-force protection
 *  - All other API endpoints:       100 requests / 1 min   — general abuse protection
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final Map<String, Bucket> authBuckets   = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String ip = resolveClientIp(request);
        String path = request.getRequestURI();

        Bucket bucket;
        if (path.startsWith("/api/auth")) {
            // Strict bucket: 10 tokens, refill 10 every 15 minutes
            bucket = authBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                    .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillGreedy(10, Duration.ofMinutes(15))
                        .build())
                    .build()
            );
        } else {
            // General bucket: 100 tokens, refill 100 every minute
            bucket = generalBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                    .addLimit(Bandwidth.builder()
                        .capacity(100)
                        .refillGreedy(100, Duration.ofMinutes(1))
                        .build())
                    .build()
            );
        }

        if (bucket.tryConsume(1)) {
            // Add remaining tokens header for client awareness
            response.setHeader("X-RateLimit-Remaining",
                String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP {} on path {}", ip, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            objectMapper.writeValue(response.getWriter(), Map.of(
                "error", "Too many requests",
                "message", "Rate limit exceeded. Please slow down.",
                "status", 429
            ));
        }
    }

    /**
     * Resolve the real client IP, respecting X-Forwarded-For when behind a trusted proxy.
     * In production: validate that the X-Forwarded-For header comes from a known proxy.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Take only the first (leftmost) IP — the actual client
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
