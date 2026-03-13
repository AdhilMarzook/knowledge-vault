package com.knowledgevault.service.provider;

/**
 * Common contract for every AI provider.
 * Each provider knows how to call its own API and return the raw text response.
 */
public interface AiProvider {

    /** Human-readable name shown in logs and /api/ai/status */
    String getName();

    /** Whether this provider has an API key configured */
    boolean isAvailable();

    /**
     * Send a prompt and return the raw model text.
     * @throws AiProviderException on any failure — timeout, auth error, parse failure, etc.
     */
    String complete(String prompt) throws AiProviderException;

    class AiProviderException extends Exception {
        private final String providerName;
        public AiProviderException(String providerName, String message, Throwable cause) {
            super(message, cause);
            this.providerName = providerName;
        }
        public AiProviderException(String providerName, String message) {
            super(message);
            this.providerName = providerName;
        }
        public String getProviderName() { return providerName; }
    }
}
