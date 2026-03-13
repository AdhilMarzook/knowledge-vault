package com.knowledgevault.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Dedicated security audit logger.
 *
 * All security-significant events are routed through here so they:
 *   1. Go to a distinct logger name (can be redirected to a separate appender/SIEM)
 *   2. Are consistently structured for machine parsing
 *   3. Never include raw passwords, tokens, or full usernames
 *
 * In production: configure logback to write AUDIT events to a tamper-evident
 * sink (e.g. CloudWatch Logs, Splunk, ELK) separate from application logs.
 */
@Component
public class AuditLogger {

    private static final Logger AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");

    public enum Event {
        REGISTER_SUCCESS,
        REGISTER_FAILURE,
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGIN_LOCKED,
        LOGOUT,
        TOKEN_REFRESH,
        TOKEN_INVALID,
        RATE_LIMIT_HIT,
        ACCESS_DENIED,
        SUSPICIOUS_INPUT,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        QUEST_ANSWERED,
    }

    /**
     * Log a security event with structured fields.
     *
     * @param event     The security event type
     * @param username  Masked username (never raw)
     * @param ip        Client IP address
     * @param detail    Optional extra detail (must NOT contain sensitive data)
     */
    public void log(Event event, String username, String ip, String detail) {
        MDC.put("audit_event", event.name());
        MDC.put("client_ip", sanitizeForLog(ip));
        MDC.put("username_masked", maskUsername(username));
        MDC.put("timestamp", Instant.now().toString());

        String message = String.format("[AUDIT] event=%s ip=%s user=%s%s",
            event.name(),
            sanitizeForLog(ip),
            maskUsername(username),
            detail != null ? " detail=" + sanitizeForLog(detail) : ""
        );

        switch (event) {
            case LOGIN_FAILURE, RATE_LIMIT_HIT, SUSPICIOUS_INPUT, ACCESS_DENIED ->
                AUDIT.warn(message);
            case ACCOUNT_LOCKED, TOKEN_INVALID ->
                AUDIT.error(message);
            default ->
                AUDIT.info(message);
        }

        MDC.clear();
    }

    public void log(Event event, String username, String ip) {
        log(event, username, ip, null);
    }

    /** Strip log-injection characters (newlines, ANSI escapes) from log strings */
    private String sanitizeForLog(String input) {
        if (input == null) return "null";
        return input
            .replaceAll("[\r\n]", "_")          // Prevent log injection
            .replaceAll("\\x1B\\[[0-9;]*m", "") // Strip ANSI escape codes
            .substring(0, Math.min(input.length(), 100)); // Truncate long values
    }

    /** Mask username so PII is not stored in logs */
    private String maskUsername(String username) {
        if (username == null || username.length() < 2) return "***";
        return username.charAt(0) + "***" + username.charAt(username.length() - 1);
    }
}
