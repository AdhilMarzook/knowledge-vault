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
 * Groq — PRIMARY free provider.
 * Extremely fast (LPU inference), generous free tier.
 * Model: llama-3.3-70b-versatile
 * Sign up free: https://console.groq.com
 */
@Component
public class GroqProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GroqProvider.class);
    private static final String BASE_URL = "https://api.groq.com/openai/v1";
    private static final String MODEL    = "llama-3.3-70b-versatile";
    private static final int    TIMEOUT  = 20;

    private final String apiKey;
    private final WebClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public GroqProvider(@Value("${ai.groq.api-key:}") String apiKey) {
        this.apiKey  = apiKey;
        this.client  = WebClient.builder()
            .baseUrl(BASE_URL)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();
    }

    @Override public String  getName()       { return "Groq / Llama-3.3-70B [FREE]"; }
    @Override public boolean isAvailable()   { return apiKey != null && !apiKey.isBlank(); }

    @Override
    public String complete(String prompt) throws AiProviderException {
        if (!isAvailable()) throw new AiProviderException(getName(), "API key not configured");
        try {
            Map<String, Object> body = Map.of(
                "model",       MODEL,
                "max_tokens",  600,
                "temperature", 0.7,
                "messages",    List.of(Map.of("role", "user", "content", prompt))
            );
            String raw = client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type",  "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(TIMEOUT))
                .block();

            JsonNode root = mapper.readTree(raw);
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (WebClientResponseException e) {
            throw new AiProviderException(getName(),
                "HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new AiProviderException(getName(), e.getMessage(), e);
        }
    }
}
