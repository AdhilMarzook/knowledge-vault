package com.knowledgevault.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Anthropic Claude — paid fallback of last resort.
 * Model: claude-haiku-4-5-20251001 (~$0.001/quest)
 * Sign up: https://console.anthropic.com
 */
@Component
public class ClaudeProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(ClaudeProvider.class);
    private static final String BASE_URL = "https://api.anthropic.com";
    private static final String MODEL    = "claude-haiku-4-5-20251001";

    private final String apiKey;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ClaudeProvider(@Value("${ai.claude.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
            .baseUrl(BASE_URL)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();
    }

    @Override public String getName() { return "Claude Haiku 4.5 [PAID]"; }

    @Override public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    @Override
    public String complete(String prompt, int maxTokens) {
        if (!isConfigured()) throw new IllegalStateException("Claude API key not configured");
        try {
            Map<String, Object> body = Map.of(
                "model", MODEL,
                "max_tokens", maxTokens,
                "messages", List.of(Map.of("role", "user", "content", prompt))
            );
            String response = webClient.post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();
            JsonNode root = objectMapper.readTree(response);
            // Claude uses different response format from OpenAI-compatible APIs
            return root.path("content").get(0).path("text").asText();
        } catch (Exception e) {
            throw new RuntimeException("Claude request failed: " + e.getMessage(), e);
        }
    }
}
