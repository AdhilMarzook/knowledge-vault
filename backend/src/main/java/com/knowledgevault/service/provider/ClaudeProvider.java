package com.knowledgevault.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Anthropic Claude — PAID last-resort fallback.
 * Only reached when ALL free providers fail.
 * Uses cheapest model (Haiku 4.5) to minimise cost (~$0.001/quest).
 * Sign up: https://console.anthropic.com
 */
@Component
public class ClaudeProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(ClaudeProvider.class);
    private static final String BASE_URL = "https://api.anthropic.com";
    private static final String MODEL    = "claude-haiku-4-5-20251001";
    private static final int    TIMEOUT  = 30;

    private final String apiKey;
    private final WebClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public ClaudeProvider(@Value("${ai.claude.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.client = WebClient.builder()
            .baseUrl(BASE_URL)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();
    }

    @Override public String  getName()     { return "Claude Haiku 4.5 [PAID - last resort]"; }
    @Override public boolean isAvailable() { return apiKey != null && !apiKey.isBlank(); }

    @Override
    public String complete(String prompt) throws AiProviderException {
        if (!isAvailable()) throw new AiProviderException(getName(), "API key not configured");
        try {
            // Claude uses a different request/response format from OpenAI
            Map<String, Object> body = Map.of(
                "model",      MODEL,
                "max_tokens", 600,
                "messages",   List.of(Map.of("role", "user", "content", prompt))
            );
            String raw = client.post()
                .uri("/v1/messages")
                .header("x-api-key",         apiKey)
                .header("anthropic-version",  "2023-06-01")
                .header("Content-Type",       "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(TIMEOUT))
                .block();

            JsonNode root = mapper.readTree(raw);
            // Claude response: content[0].text
            return root.path("content").get(0).path("text").asText();

        } catch (WebClientResponseException e) {
            throw new AiProviderException(getName(),
                "HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new AiProviderException(getName(), e.getMessage(), e);
        }
    }
}
