package com.knowledgevault.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a unique Request-ID to every incoming request.
 *
 * Benefits:
 *  - All log lines for a single request share the same requestId in MDC
 *  - requestId is returned in the response header for client-side correlation
 *  - Security audit events can be traced across services
 *  - Prevents log-injection by generating the ID internally (not from client headers)
 *
 * Order: runs first in the filter chain (before rate limiting and JWT auth).
 */
@Component
@Order(1)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Always generate our own request ID — never trust a client-supplied one
        // (A client could inject values to poison logs)
        String requestId = UUID.randomUUID().toString();

        // Store in MDC so all log lines for this request automatically include it
        MDC.put(MDC_KEY, requestId);

        // Echo back in response header so clients can correlate with their own logs
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // ALWAYS clear MDC — thread pool reuse would leak stale request IDs
            MDC.remove(MDC_KEY);
        }
    }
}
