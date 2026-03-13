package com.knowledgevault.service.provider;

/**
 * Common contract for every AI provider.
 * Each provider knows how to call its own API and return the raw text response.
 */
public interface AiProvider {

    /** Human-readable name shown in logs and /api/providers/status */
    String getName();

    /** Whether this provider has an API key configured */
    boolean isConfigured();

    /**
     * Send a prompt and return the raw model text.
     * @param prompt    the full prompt to send
     * @param maxTokens maximum tokens to generate
     * @throws RuntimeException on any failure — timeout, auth error, parse failure, etc.
     */
    String complete(String prompt, int maxTokens);
}
