package gov.prajadisha.backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin client for the <a href="https://ollama.com">Ollama Cloud</a> (and self-hosted Ollama)
 * REST API. Talks to {@code /api/chat} for text/structured completions and {@code /api/embed}
 * for vector embeddings.
 *
 * <p>Configuration (see {@code application.properties}):
 * <ul>
 *   <li>{@code ollama.base-url}   — default {@code https://ollama.com}</li>
 *   <li>{@code ollama.api-key}    — your Ollama Cloud API key (sent as {@code Authorization: Bearer})</li>
 *   <li>{@code ollama.chat-model} — e.g. {@code gpt-oss:120b}</li>
 *   <li>{@code ollama.embed-model}— e.g. {@code embeddinggemma}</li>
 *   <li>{@code ollama.enabled}    — set {@code false} to force the deterministic fallbacks</li>
 * </ul>
 *
 * <p>All calls are best-effort: on any error they throw {@link OllamaException} so callers can
 * fall back to deterministic behaviour and keep the app functional even without a key/network.
 */
@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final RestClient chatHttp;
    private final RestClient embedHttp;
    private final ObjectMapper mapper;

    private final boolean enabled;
    private final String apiKey;
    private final String chatModel;
    private final String embedModel;
    private final boolean hasCustomEmbedUrl;

    public OllamaClient(
            ObjectMapper mapper,
            @Value("${ollama.enabled:true}") boolean enabled,
            @Value("${ollama.base-url:https://ollama.com}") String baseUrl,
            @Value("${ollama.embed-base-url:}") String embedBaseUrl,
            @Value("${ollama.api-key:}") String apiKey,
            @Value("${ollama.chat-model:gpt-oss:120b}") String chatModel,
            @Value("${ollama.embed-model:embeddinggemma}") String embedModel,
            @Value("${ollama.timeout-seconds:60}") int timeoutSeconds) {
        this.mapper = mapper;
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.chatModel = chatModel;
        this.embedModel = embedModel;
        this.hasCustomEmbedUrl = embedBaseUrl != null && !embedBaseUrl.isBlank();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(15).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(Math.max(timeoutSeconds, 5)).toMillis());

        // Chat REST Client configuration
        RestClient.Builder chatBuilder = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl == null ? "https://ollama.com" : baseUrl.replaceAll("/+$", ""));
        if (!this.apiKey.isEmpty()) {
            chatBuilder.defaultHeader("Authorization", "Bearer " + this.apiKey);
        }
        this.chatHttp = chatBuilder.build();

        // Embed REST Client configuration
        String finalEmbedUrl = this.hasCustomEmbedUrl ? embedBaseUrl : (baseUrl == null ? "https://ollama.com" : baseUrl);
        RestClient.Builder embedBuilder = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(finalEmbedUrl.replaceAll("/+$", ""));
        if (!this.apiKey.isEmpty() && !this.hasCustomEmbedUrl) {
            embedBuilder.defaultHeader("Authorization", "Bearer " + this.apiKey);
        }
        this.embedHttp = embedBuilder.build();
    }

    /** True when chat/triage AI calls should be attempted. */
    public boolean isChatEnabled() {
        return enabled && !apiKey.isEmpty();
    }

    /** True when embedding vector generation is enabled. */
    public boolean isEmbedEnabled() {
        return enabled && (!apiKey.isEmpty() || hasCustomEmbedUrl);
    }

    /** True when AI calls should be attempted (for backward compatibility). */
    public boolean isEnabled() {
        return isChatEnabled();
    }

    public String chat(String system, String user) {
        return chat(system, null, user);
    }

    public String chat(String system, List<Map<String, String>> history, String user) {
        return chatInternal(system, history, user, null).path("message").path("content").asText("");
    }

    public JsonNode chatJson(String system, String user, Map<String, Object> jsonSchema) {
        return chatJson(system, null, user, jsonSchema);
    }

    public JsonNode chatJson(String system, List<Map<String, String>> history, String user, Map<String, Object> jsonSchema) {
        Object format = jsonSchema != null ? jsonSchema : "json";
        JsonNode response = chatInternal(system, history, user, format);
        String content = response.path("message").path("content").asText("");
        try {
            return mapper.readTree(content);
        } catch (Exception e) {
            throw new OllamaException("Model did not return valid JSON: " + content, e);
        }
    }

    private JsonNode chatInternal(String system, List<Map<String, String>> history, String user, Object format) {
        if (!isChatEnabled()) {
            throw new OllamaException("Ollama chat is disabled or no API key configured");
        }
        List<Map<String, String>> messages = new ArrayList<>();
        if (system != null && !system.isBlank()) {
            messages.add(Map.of("role", "system", "content", system));
        }
        if (history != null) {
            messages.addAll(history);
        }
        messages.add(Map.of("role", "user", "content", user == null ? "" : user));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", chatModel);
        body.put("messages", messages);
        body.put("stream", false);
        if (format != null) {
            body.put("format", format);
        }

        try {
            String raw = chatHttp.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return mapper.readTree(raw == null ? "{}" : raw);
        } catch (OllamaException e) {
            throw e;
        } catch (Exception e) {
            throw new OllamaException("Ollama /api/chat call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Produces a vector embedding for the given text. Best-effort — returns an empty list (and logs)
     * if the embedding model is unavailable, so it never blocks ticket processing.
     */
    public List<Double> embed(String text) {
        if (!isEmbedEnabled() || text == null || text.isBlank()) {
            return List.of();
        }
        Map<String, Object> body = Map.of("model", embedModel, "input", text);
        try {
            String raw = embedHttp.post()
                    .uri("/api/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode node = mapper.readTree(raw == null ? "{}" : raw);
            JsonNode vectors = node.path("embeddings");
            if (vectors.isArray() && vectors.size() > 0 && vectors.get(0).isArray()) {
                List<Double> out = new ArrayList<>(vectors.get(0).size());
                vectors.get(0).forEach(v -> out.add(v.asDouble()));
                return out;
            }
            return List.of();
        } catch (Exception e) {
            log.warn("Ollama embedding unavailable ({}); continuing without a vector.", e.getMessage());
            return List.of();
        }
    }

    /** Raised when an Ollama call cannot be completed; callers fall back to deterministic logic. */
    public static class OllamaException extends RuntimeException {
        public OllamaException(String message) {
            super(message);
        }

        public OllamaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
