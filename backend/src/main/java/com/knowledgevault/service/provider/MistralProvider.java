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
 * Mistral AI — Free Experiment plan (unlimited requests, rate limited).
 * Sign up: https://console.mistral.ai
 */
@Component
public class MistralProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(MistralProvider.class);
    private static final String BASE_URL = "https://api.mistral.ai/v1";
    private static final String MODEL    = "mistral-small-latest";

    private final String apiKey;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public MistralProvider(@Value("${ai.mistral.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
            .baseUrl(BASE_URL)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();
    }

    @Override public String getName() { return "Mistral Small [FREE]"; }

    @Override public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    @Override
    public String complete(String prompt, int maxTokens) {
        if (!isConfigured()) throw new IllegalStateException("Mistral API key not configured");
        try {
            Map<String, Object> body = Map.of(
                "model", MODEL,
                "max_tokens", maxTokens,
                "temperature", 0.7,
                "messages", List.of(Map.of("role", "user", "content", prompt))
            );
            String response = webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();
            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Mistral request failed: " + e.getMessage(), e);
        }
    }
}
