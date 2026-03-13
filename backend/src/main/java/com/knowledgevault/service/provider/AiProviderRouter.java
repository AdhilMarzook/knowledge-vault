package com.knowledgevault.service.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Provider Router — the heart of the multi-provider system.
 *
 * Fallback chain (in order):
 *   1. Groq         (free, fastest — Llama 3.3 70B)
 *   2. Gemini        (free, ~500 req/day — Gemini 2.0 Flash)
 *   3. Mistral       (free, rate limited — Mistral Small)
 *   4. OpenRouter    (free models — Llama 3.3 70B via OpenRouter)
 *   5. Claude Haiku  (paid last resort — ~$0.001/quest)
 *
 * Skips providers that:
 *   - Have no API key configured
 *   - Are in circuit-breaker cooldown
 *
 * Throws RuntimeException only if ALL configured providers fail.
 */
@Service
public class AiProviderRouter {

    private static final Logger log = LoggerFactory.getLogger(AiProviderRouter.class);

    private final List<AiProvider> providerChain;
    private final ProviderHealthTracker healthTracker;

    public AiProviderRouter(
        GroqProvider groq,
        GeminiProvider gemini,
        MistralProvider mistral,
        OpenRouterProvider openRouter,
        ClaudeProvider claude,
        ProviderHealthTracker healthTracker
    ) {
        // Order matters — free providers first, paid last
        this.providerChain = List.of(groq, gemini, mistral, openRouter, claude);
        this.healthTracker = healthTracker;
    }

    /**
     * Try each provider in order, fall through on failure.
     * @return the raw text response from the first successful provider
     */
    public String complete(String prompt, int maxTokens) {
        StringBuilder attempts = new StringBuilder();

        for (AiProvider provider : providerChain) {
            if (!provider.isConfigured()) {
                log.debug("Skipping {} — not configured", provider.getName());
                continue;
            }

            if (!healthTracker.isAvailable(provider.getName())) {
                log.debug("Skipping {} — in circuit-breaker cooldown", provider.getName());
                attempts.append(provider.getName()).append(" [cooldown], ");
                continue;
            }

            try {
                log.info("Attempting AI request via {}", provider.getName());
                String result = provider.complete(prompt, maxTokens);
                healthTracker.recordSuccess(provider.getName());
                log.info("AI request succeeded via {}", provider.getName());
                return result;
            } catch (Exception e) {
                healthTracker.recordFailure(provider.getName());
                log.warn("Provider {} failed: {} — trying next", provider.getName(), e.getMessage());
                attempts.append(provider.getName()).append(" [failed: ")
                    .append(e.getClass().getSimpleName()).append("], ");
            }
        }

        throw new RuntimeException(
            "All AI providers exhausted. Attempted: " + attempts +
            " — Check API keys and rate limits."
        );
    }

    /** Returns which providers are configured and their health status */
    public Map<String, Object> getProviderStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        for (AiProvider provider : providerChain) {
            Map<String, Object> providerInfo = new LinkedHashMap<>();
            providerInfo.put("configured", provider.isConfigured());
            providerInfo.put("health", healthTracker.getStatusSummary(provider.getName()));
            status.put(provider.getName(), providerInfo);
        }
        return status;
    }

    /** Returns the name of the first available configured provider */
    public String getActiveProviderName() {
        return providerChain.stream()
            .filter(p -> p.isConfigured() && healthTracker.isAvailable(p.getName()))
            .map(AiProvider::getName)
            .findFirst()
            .orElse("None available");
    }
}
