package com.knowledgevault.service.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Smart AI provider orchestrator.
 *
 * Priority order (free-first):
 *   1. Groq          — fastest, free
 *   2. Gemini         — free, ~500 req/day
 *   3. OpenRouter     — free models via unified API
 *   4. Mistral        — free, rate-limited (2 req/min)
 *   5. Claude Haiku   — paid last resort (~$0.001/quest)
 *
 * Features:
 *  - Automatic failover: if a provider fails, tries the next
 *  - Per-provider circuit breaker: after 3 consecutive failures,
 *    a provider is cooled off for 5 minutes before retrying
 *  - Usage tracking: counts calls per provider for /api/ai/status
 *  - Skips unconfigured providers silently
 */
@Service
public class ProviderOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ProviderOrchestrator.class);

    private static final int  CIRCUIT_FAILURE_THRESHOLD = 3;
    private static final long CIRCUIT_COOLDOWN_SECONDS  = 300; // 5 minutes

    // Ordered list — first = highest priority
    private final List<AiProvider> chain;

    // Circuit breaker state per provider
    private final Map<String, ProviderState> states = new ConcurrentHashMap<>();

    public ProviderOrchestrator(
        GroqProvider groq,
        GeminiProvider gemini,
        OpenRouterProvider openRouter,
        MistralProvider mistral,
        ClaudeProvider claude
    ) {
        this.chain = List.of(groq, gemini, openRouter, mistral, claude);
        chain.forEach(p -> states.put(p.getName(), new ProviderState()));
    }

    /**
     * Try each configured provider in priority order.
     * Falls back automatically on any failure.
     * Throws if all providers fail.
     */
    public ProviderResponse complete(String prompt) {
        List<String> attempted = new ArrayList<>();

        for (AiProvider provider : chain) {
            if (!provider.isAvailable()) continue;

            ProviderState state = states.get(provider.getName());
            if (state.isCoolingDown()) {
                log.debug("Skipping {} — in circuit breaker cooldown", provider.getName());
                continue;
            }

            attempted.add(provider.getName());
            try {
                String text = provider.complete(prompt);
                state.recordSuccess();
                log.info("Quest generated via {}", provider.getName());
                return new ProviderResponse(text, provider.getName());

            } catch (AiProvider.AiProviderException e) {
                state.recordFailure();
                log.warn("Provider {} failed (attempt {}/{}): {}",
                    provider.getName(), state.consecutiveFailures,
                    CIRCUIT_FAILURE_THRESHOLD, e.getMessage());
            }
        }

        throw new AllProvidersFailedException(
            "All AI providers failed or unavailable. Attempted: " + attempted
        );
    }

    /** Returns current status of all providers for the status endpoint */
    public List<Map<String, Object>> getProviderStatus() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiProvider p : chain) {
            ProviderState state = states.get(p.getName());
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name",               p.getName());
            info.put("configured",         p.isAvailable());
            info.put("totalCalls",         state.totalCalls);
            info.put("successCalls",       state.successCalls);
            info.put("consecutiveFailures",state.consecutiveFailures);
            info.put("coolingDown",        state.isCoolingDown());
            info.put("status",             resolveStatus(p, state));
            result.add(info);
        }
        return result;
    }

    private String resolveStatus(AiProvider p, ProviderState state) {
        if (!p.isAvailable())      return "NOT_CONFIGURED";
        if (state.isCoolingDown()) return "CIRCUIT_OPEN";
        if (state.consecutiveFailures > 0) return "DEGRADED";
        return "OK";
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    public record ProviderResponse(String text, String usedProvider) {}

    public static class AllProvidersFailedException extends RuntimeException {
        public AllProvidersFailedException(String msg) { super(msg); }
    }

    private static class ProviderState {
        int consecutiveFailures = 0;
        long totalCalls         = 0;
        long successCalls       = 0;
        Instant cooldownUntil   = null;

        void recordSuccess() {
            consecutiveFailures = 0;
            totalCalls++;
            successCalls++;
            cooldownUntil = null;
        }

        void recordFailure() {
            consecutiveFailures++;
            totalCalls++;
            if (consecutiveFailures >= CIRCUIT_FAILURE_THRESHOLD) {
                cooldownUntil = Instant.now().plusSeconds(CIRCUIT_COOLDOWN_SECONDS);
                LoggerFactory.getLogger(ProviderOrchestrator.class)
                    .warn("Circuit breaker OPEN — cooling down for {} seconds", CIRCUIT_COOLDOWN_SECONDS);
            }
        }

        boolean isCoolingDown() {
            if (cooldownUntil == null) return false;
            if (Instant.now().isAfter(cooldownUntil)) {
                // Auto-reset after cooldown
                consecutiveFailures = 0;
                cooldownUntil = null;
                return false;
            }
            return true;
        }
    }
}
