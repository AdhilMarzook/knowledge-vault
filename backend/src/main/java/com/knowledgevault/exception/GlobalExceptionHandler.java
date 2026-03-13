package com.knowledgevault.exception;

import com.knowledgevault.security.AuthService;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralised error handling.
 *
 * Rules:
 *  - NEVER include stack traces, internal class names, or DB details in responses
 *  - Log full detail server-side; return minimal safe message to client
 *  - Return consistent JSON shape for all errors
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Validation failures ───────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Invalid input", null);
    }

    // ── Auth failures ─────────────────────────────────────────────────────────

    @ExceptionHandler(AuthService.AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuthException(AuthService.AuthException ex) {
        // Log without full stack — message is already safe
        log.debug("Auth failure: {}", ex.getMessage());
        return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        log.debug("Spring Security auth exception: {}", ex.getClass().getSimpleName());
        return buildError(HttpStatus.UNAUTHORIZED, "Authentication required", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN, "Access denied", null);
    }

    // ── Catch-all — NEVER leak internals ─────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {
        // Full detail logged server-side only
        log.error("Unhandled exception [{}]: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred", null);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> buildError(
        HttpStatus status, String message, Object details
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());
        if (details != null) body.put("details", details);
        return ResponseEntity.status(status).body(body);
    }
}
