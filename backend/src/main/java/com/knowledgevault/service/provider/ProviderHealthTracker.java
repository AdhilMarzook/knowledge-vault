package com.knowledgevault.service.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight circuit-breaker tracker.
 *
 * When a provider fails 3 consecutive times it enters a 5-minute cooldown.
 * After the cooldown it gets one probe attempt; if it succeeds, it fully recovers.
 * This prevents hammering a rate-limited provider on every request.
 */
@Component
public class ProviderHealthTracker {

    private static final Logger log = LoggerFactory.getLogger(ProviderHealthTracker.class);

    private static final int  FAILURE_THRESHOLD   = 3;
    private static final long COOLDOWN_SECONDS    = 300; // 5 minutes

    private record ProviderState(AtomicInteger failures, Instant cooldownUntil) {
        ProviderState() { this(new AtomicInteger(0), null); }
    }

    private final Map<String, AtomicInteger> failureCounts = new ConcurrentHashMap<>();
    private final Map<String, Instant>       cooldownUntil = new ConcurrentHashMap<>();
    private final Map<String, Long>          totalCalls    = new ConcurrentHashMap<>();
    private final Map<String, Long>          totalSuccesses = new ConcurrentHashMap<>();

    public boolean isAvailable(String providerName) {
        Instant cooldown = cooldownUntil.get(providerName);
        if (cooldown != null && Instant.now().isBefore(cooldown)) {
            log.debug("Provider {} is in cooldown until {}", providerName, cooldown);
            return false;
        }
        return true;
    }

    public void recordSuccess(String providerName) {
        failureCounts.remove(providerName);
        cooldownUntil.remove(providerName);
        totalSuccesses.merge(providerName, 1L, Long::sum);
        totalCalls.merge(providerName, 1L, Long::sum);
    }

    public void recordFailure(String providerName) {
        totalCalls.merge(providerName, 1L, Long::sum);
        int failures = failureCounts
            .computeIfAbsent(providerName, k -> new AtomicInteger(0))
            .incrementAndGet();

        if (failures >= FAILURE_THRESHOLD) {
            Instant until = Instant.now().plusSeconds(COOLDOWN_SECONDS);
            cooldownUntil.put(providerName, until);
            failureCounts.get(providerName).set(0);
            log.warn("Provider {} tripped circuit breaker after {} failures. Cooldown until {}",
                providerName, failures, until);
        }
    }

    /** Returns a summary map for the /api/providers/status endpoint */
    public Map<String, Object> getStatusSummary(String providerName) {
        Instant cooldown = cooldownUntil.get(providerName);
        boolean available = cooldown == null || Instant.now().isAfter(cooldown);
        long calls     = totalCalls.getOrDefault(providerName, 0L);
        long successes = totalSuccesses.getOrDefault(providerName, 0L);
        return Map.of(
            "available", available,
            "totalCalls", calls,
            "successRate", calls > 0 ? String.format("%.0f%%", (successes * 100.0) / calls) : "N/A",
            "cooldownUntil", cooldown != null ? cooldown.toString() : "none"
        );
    }
}
