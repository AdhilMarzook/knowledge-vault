package com.knowledgevault.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Centralised input sanitizer.
 *
 * Applied to all user-supplied strings before they are stored or used in
 * any query / API call. Operates in two modes:
 *
 *  sanitize()       — strips control chars, null bytes, and HTML-dangerous chars
 *  sanitizeStrict() — like sanitize() but also enforces alphanumeric + safe symbols only
 *
 * This is a defence-in-depth layer — Bean Validation (@Valid) runs first,
 * but we also sanitize here to handle any path that bypasses validation.
 */
@Component
public class InputSanitizer {

    // Null byte and other control characters (except tab/newline used legitimately)
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    // HTML-dangerous characters — prevent XSS if output is ever reflected in HTML
    private static final Pattern HTML_DANGEROUS = Pattern.compile("[<>&\"'`]");

    // Script injection patterns
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "(?i)(script|javascript|vbscript|onload|onerror|onclick|eval\\s*\\(|expression\\s*\\()",
        Pattern.CASE_INSENSITIVE
    );

    // SQL injection keywords (defence-in-depth, since we don't use SQL directly yet)
    private static final Pattern SQL_KEYWORDS = Pattern.compile(
        "(?i)(--|;|/\\*|\\*/|xp_|UNION\\s+SELECT|DROP\\s+TABLE|INSERT\\s+INTO|DELETE\\s+FROM|UPDATE\\s+SET)",
        Pattern.CASE_INSENSITIVE
    );

    // Path traversal
    private static final Pattern PATH_TRAVERSAL = Pattern.compile("(\\.\\./|%2e%2e|%252e)");

    /**
     * General sanitization — removes control characters and HTML metacharacters.
     * Safe for usernames, messages, and general text fields.
     */
    public String sanitize(String input) {
        if (input == null) return null;

        String cleaned = input.trim();
        cleaned = CONTROL_CHARS.matcher(cleaned).replaceAll("");
        cleaned = HTML_DANGEROUS.matcher(cleaned).replaceAll("");
        cleaned = PATH_TRAVERSAL.matcher(cleaned).replaceAll("");

        // Detect but reject (don't silently strip) script/SQL injection
        if (SCRIPT_PATTERN.matcher(cleaned).find() || SQL_KEYWORDS.matcher(cleaned).find()) {
            throw new SecurityException("Input contains disallowed patterns");
        }

        return cleaned;
    }

    /**
     * Strict sanitization — only allows alphanumeric characters, spaces, and safe punctuation.
     * Use for usernames and identifiers.
     */
    public String sanitizeUsername(String input) {
        if (input == null) return null;
        String cleaned = input.trim();
        // Only allow: letters, digits, hyphen, underscore
        if (!cleaned.matches("^[a-zA-Z0-9_-]{3,20}$")) {
            throw new SecurityException("Username contains invalid characters");
        }
        return cleaned;
    }

    /**
     * Validates and returns a skill name only if it matches the known whitelist.
     * Never used in query construction, but sanitized anyway for defence-in-depth.
     */
    public String sanitizeSkill(String input, java.util.Set<String> allowedSkills) {
        if (input == null) throw new SecurityException("Skill cannot be null");
        String cleaned = input.trim();
        if (!allowedSkills.contains(cleaned)) {
            throw new SecurityException("Unknown skill: " + cleaned.replaceAll("[^a-zA-Z]", ""));
        }
        return cleaned;
    }

    /**
     * Truncate input to a maximum length to prevent buffer-overflow style exploits
     * in downstream systems.
     */
    public String truncate(String input, int maxLength) {
        if (input == null) return null;
        return input.length() > maxLength ? input.substring(0, maxLength) : input;
    }
}
