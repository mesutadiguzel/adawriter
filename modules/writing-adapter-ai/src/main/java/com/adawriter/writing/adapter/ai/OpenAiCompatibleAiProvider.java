package com.adawriter.writing.adapter.ai;

import com.adawriter.writing.domain.AiCompletionCommand;
import com.adawriter.writing.domain.AiCompletionResult;
import com.adawriter.writing.domain.AiProviderException;
import com.adawriter.writing.domain.AiProviderPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

/**
 * OpenAI-compatible chat completions adapter (cloud or local OpenAI-compatible server).
 */
public final class OpenAiCompatibleAiProvider implements AiProviderPort {

    public static final String PROVIDER_ID = "openai-compatible";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI endpoint;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public OpenAiCompatibleAiProvider(URI endpoint, String apiKey, String model, Duration timeout) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.objectMapper = new ObjectMapper();
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.model = Objects.requireNonNull(model, "model");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    @Override
    public AiCompletionResult complete(AiCompletionCommand command) {
        long started = System.nanoTime();
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", command.maxTokens());
            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "system").put("content", command.systemPrompt());
            messages.addObject().put("role", "user").put("content", command.userPrompt());

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            if (!apiKey.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> response =
                    httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiProviderException("AI provider HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new AiProviderException("AI provider response missing content");
            }
            String usedModel = root.path("model").asText(model);
            long latencyMs = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
            return new AiCompletionResult(content.asText(), usedModel, latencyMs);
        } catch (AiProviderException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new AiProviderException("AI provider I/O failure", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("AI provider call interrupted", ex);
        }
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }
}
