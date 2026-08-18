package com.adawriter.writing.adapter.rest;

import com.adawriter.privacy.application.PrivacyGuard;
import com.adawriter.privacy.domain.DetectionResult;
import com.adawriter.privacy.domain.RedactionPolicy;
import com.adawriter.privacy.domain.RedactionResult;
import com.adawriter.privacy.domain.SensitiveContentBlockedException;
import com.adawriter.privacy.domain.SensitiveSpan;
import com.adawriter.writing.application.AssistWritingUseCase;
import com.adawriter.writing.application.WritingMetrics;
import com.adawriter.writing.domain.AiProviderException;
import com.adawriter.writing.domain.ValidationException;
import com.adawriter.writing.domain.WritingAction;
import com.adawriter.writing.domain.WritingConstraints;
import com.adawriter.writing.domain.WritingException;
import com.adawriter.writing.domain.WritingRequest;
import com.adawriter.writing.domain.WritingResult;
import com.adawriter.writing.domain.WritingTone;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Localhost REST adapter for the desktop agent.
 */
public final class LocalWritingHttpServer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(LocalWritingHttpServer.class);
    private static final int MAX_BODY_BYTES = WritingConstraints.MAX_TEXT_CHARS * 4;

    private final AssistWritingUseCase assistWriting;
    private final PrivacyGuard privacyGuard;
    private final WritingMetrics metrics;
    private final ObjectMapper objectMapper;
    private final int port;
    private HttpServer server;
    private ExecutorService executor;

    public LocalWritingHttpServer(
            AssistWritingUseCase assistWriting, PrivacyGuard privacyGuard, WritingMetrics metrics, int port) {
        this.assistWriting = Objects.requireNonNull(assistWriting, "assistWriting");
        this.privacyGuard = Objects.requireNonNull(privacyGuard, "privacyGuard");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.objectMapper = new ObjectMapper();
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port out of range");
        }
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.createContext("/health", this::health);
        server.createContext("/metrics", this::metrics);
        server.createContext("/v1/assist", this::assist);
        server.createContext("/v1/privacy/detect", this::detect);
        server.createContext("/v1/privacy/redact", this::redact);
        server.setExecutor(executor);
        server.start();
        log.info("local_writing_http_started bind=127.0.0.1:{}", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ex) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
        log.info("local_writing_http_stopped");
    }

    @Override
    public void close() {
        stop();
    }

    public int port() {
        return port;
    }

    private void health(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange);
            return;
        }
        writeJson(exchange, 200, objectMapper.createObjectNode().put("status", "UP"));
    }

    private void metrics(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange);
            return;
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("assistRequests", metrics.assistRequests());
        body.put("assistFailures", metrics.assistFailures());
        body.put("totalLatencyMs", metrics.totalLatencyMs());
        writeJson(exchange, 200, body);
    }

    private void detect(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange);
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(readBody(exchange));
            String text = textOrThrow(root, "text");
            DetectionResult detection = privacyGuard.detect(text);
            writeJson(exchange, 200, toDetectionJson(detection));
        } catch (ValidationException | IllegalArgumentException ex) {
            writeError(exchange, 400, safeClientMessage(ex));
        } catch (RuntimeException ex) {
            log.warn("detect_unhandled type={} reason={}", ex.getClass().getSimpleName(), ex.getMessage());
            writeError(exchange, 500, "Internal error");
        }
    }

    private void redact(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange);
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(readBody(exchange));
            String text = textOrThrow(root, "text");
            RedactionPolicy policy = RedactionPolicy.REDACT;
            if (root.hasNonNull("policy")) {
                policy = RedactionPolicy.valueOf(
                        root.get("policy").asText().trim().toUpperCase(Locale.ROOT));
            }
            RedactionResult result = privacyGuard.protect(text, policy);
            ObjectNode body = toDetectionJson(result.detection());
            body.put("redactedText", result.text());
            body.put("policy", result.policyApplied().name());
            writeJson(exchange, 200, body);
        } catch (SensitiveContentBlockedException ex) {
            ObjectNode body = toDetectionJson(ex.detection());
            body.put("error", "Sensitive content blocked by privacy policy");
            writeJson(exchange, 422, body);
        } catch (ValidationException | IllegalArgumentException ex) {
            writeError(exchange, 400, safeClientMessage(ex));
        } catch (RuntimeException ex) {
            log.warn("redact_unhandled type={} reason={}", ex.getClass().getSimpleName(), ex.getMessage());
            writeError(exchange, 500, "Internal error");
        }
    }

    private void assist(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange);
            return;
        }
        try {
            byte[] rawBody = readBody(exchange);
            JsonNode root = objectMapper.readTree(rawBody);
            String text = textOrThrow(root, "text");
            WritingAction action =
                    WritingAction.valueOf(textOrThrow(root, "action").trim().toUpperCase(Locale.ROOT));
            WritingRequest.Builder builder = WritingRequest.builder(text, action);
            if (root.hasNonNull("tone")) {
                builder.tone(
                        WritingTone.valueOf(root.get("tone").asText().trim().toUpperCase(Locale.ROOT)));
            }
            if (root.hasNonNull("locale")) {
                builder.locale(root.get("locale").asText());
            }
            WritingResult result = assistWriting.execute(builder.build());
            ObjectNode body = objectMapper.createObjectNode();
            body.put("outputText", result.outputText());
            body.put("providerId", result.providerId());
            body.put("modelId", result.modelId());
            body.put("promptVersion", result.promptVersion());
            body.put("latencyMs", result.latencyMs());
            writeJson(exchange, 200, body);
        } catch (SensitiveContentBlockedException ex) {
            ObjectNode body = toDetectionJson(ex.detection());
            body.put("error", "Sensitive content blocked by privacy policy");
            writeJson(exchange, 422, body);
        } catch (ValidationException | IllegalArgumentException ex) {
            writeError(exchange, 400, safeClientMessage(ex));
        } catch (AiProviderException ex) {
            writeError(exchange, 502, "AI provider unavailable");
        } catch (WritingException ex) {
            writeError(exchange, 500, "Writing assistance failed");
        } catch (RuntimeException ex) {
            log.warn("assist_unhandled type={} reason={}", ex.getClass().getSimpleName(), ex.getMessage());
            writeError(exchange, 500, "Internal error");
        }
    }

    private ObjectNode toDetectionJson(DetectionResult detection) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("findingCount", detection.findingCount());
        ArrayNode findings = body.putArray("findings");
        for (SensitiveSpan span : detection.spans()) {
            ObjectNode item = findings.addObject();
            item.put("start", span.startInclusive());
            item.put("end", span.endExclusive());
            item.put("category", span.category().name());
            item.put("token", span.redactionToken());
        }
        return body;
    }

    private static String safeClientMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Invalid request";
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }

    private static String textOrThrow(JsonNode root, String field) {
        if (!root.hasNonNull(field)) {
            throw new ValidationException("Missing field: " + field);
        }
        String value = root.get(field).asText();
        if (value.isBlank()) {
            throw new ValidationException("Blank field: " + field);
        }
        return value;
    }

    private static byte[] readBody(HttpExchange exchange) throws IOException {
        if (exchange.getRequestHeaders().containsKey("Content-Length")) {
            try {
                long declared = Long.parseLong(exchange.getRequestHeaders().getFirst("Content-Length"));
                if (declared > MAX_BODY_BYTES) {
                    throw new ValidationException("Request body too large");
                }
            } catch (NumberFormatException ex) {
                throw new ValidationException("Invalid Content-Length");
            }
        }
        try (InputStream in = exchange.getRequestBody()) {
            byte[] data = in.readNBytes(MAX_BODY_BYTES + 1);
            if (data.length > MAX_BODY_BYTES) {
                throw new ValidationException("Request body too large");
            }
            return data;
        }
    }

    private void writeError(HttpExchange exchange, int status, String message) throws IOException {
        ObjectNode body = objectMapper.createObjectNode().put("error", message == null ? "error" : message);
        writeJson(exchange, status, body);
    }

    private void methodNotAllowed(HttpExchange exchange) throws IOException {
        writeError(exchange, 405, "Method not allowed");
    }

    private void writeJson(HttpExchange exchange, int status, ObjectNode body) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
