package com.adawriter.writing.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.adawriter.privacy.application.PrivacyGuard;
import com.adawriter.privacy.domain.RedactionPolicy;
import com.adawriter.privacy.domain.SensitiveTextDetector;
import com.adawriter.writing.adapter.ai.OfflineRuleBasedAiProvider;
import com.adawriter.writing.application.AssistWritingUseCase;
import com.adawriter.writing.application.WritingMetrics;
import com.adawriter.writing.domain.AiCompletionCommand;
import com.adawriter.writing.domain.AiCompletionResult;
import com.adawriter.writing.domain.AiProviderException;
import com.adawriter.writing.domain.AiProviderPort;
import com.adawriter.writing.domain.UnexpectedWritingException;
import com.adawriter.writing.domain.ValidationException;
import com.adawriter.writing.domain.WritingConstraints;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocalWritingHttpServerTest {

    private LocalWritingHttpServer server;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() throws Exception {
        startDefaultServer();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
            server = null;
        }
    }

    @Test
    void negative_constructorRejectsInvalidPort() {
        WritingMetrics metrics = new WritingMetrics();
        PrivacyGuard privacy = PrivacyGuard.withDefaults();
        AssistWritingUseCase useCase = new AssistWritingUseCase(new OfflineRuleBasedAiProvider(), metrics, privacy);
        assertThatThrownBy(() -> new LocalWritingHttpServer(useCase, privacy, metrics, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port");
        assertThatThrownBy(() -> new LocalWritingHttpServer(useCase, privacy, metrics, 65536))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void positive_healthIsUp() throws Exception {
        HttpResponse<String> response = get("/health");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    void positive_metricsEndpointReturnsCounters() throws Exception {
        post("/v1/assist", "{\"text\":\"Hello from AdaWriter.\",\"action\":\"REWRITE\"}");
        HttpResponse<String> response = get("/metrics");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("assistRequests");
        assertThat(response.body()).contains("totalLatencyMs");
    }

    @Test
    void positive_assistRewriteWithToneAndLocale() throws Exception {
        HttpResponse<String> response = post(
                "/v1/assist",
                "{\"text\":\"Hello from AdaWriter.\",\"action\":\"REWRITE\",\"tone\":\"PROFESSIONAL\",\"locale\":\"en-GB\"}");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("outputText");
        assertThat(response.body()).contains("offline");
    }

    @Test
    void positive_detectFindsEmailWithoutLeakingRawValue() throws Exception {
        HttpResponse<String> response = post("/v1/privacy/detect", "{\"text\":\"Reach jane.doe@example.com today\"}");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("EMAIL");
        assertThat(response.body()).contains("[EMAIL]");
        assertThat(response.body()).doesNotContain("jane.doe@example.com");
    }

    @Test
    void positive_redactReplacesEmail() throws Exception {
        HttpResponse<String> response =
                post("/v1/privacy/redact", "{\"text\":\"Reach jane.doe@example.com today\",\"policy\":\"REDACT\"}");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Reach [EMAIL] today");
        assertThat(response.body()).doesNotContain("jane.doe@example.com");
    }

    @Test
    void negative_wrongMethodsReturn405() throws Exception {
        assertThat(post("/health", "{}").statusCode()).isEqualTo(405);
        assertThat(post("/metrics", "{}").statusCode()).isEqualTo(405);
        assertThat(get("/v1/assist").statusCode()).isEqualTo(405);
        assertThat(get("/v1/privacy/detect").statusCode()).isEqualTo(405);
        assertThat(get("/v1/privacy/redact").statusCode()).isEqualTo(405);
    }

    @Test
    void negative_assistRejectsMissingText() throws Exception {
        HttpResponse<String> response = post("/v1/assist", "{\"action\":\"REWRITE\"}");
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("error");
    }

    @Test
    void negative_assistRejectsUnknownAction() throws Exception {
        HttpResponse<String> response = post("/v1/assist", "{\"text\":\"Hello\",\"action\":\"DANCE\"}");
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void negative_detectRejectsBlankText() throws Exception {
        HttpResponse<String> response = post("/v1/privacy/detect", "{\"text\":\"   \"}");
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void negative_redactRejectsUnknownPolicy() throws Exception {
        HttpResponse<String> response = post("/v1/privacy/redact", "{\"text\":\"hello\",\"policy\":\"YEET\"}");
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void negative_redactBlockReturns422() throws Exception {
        restartWith(new OfflineRuleBasedAiProvider(), RedactionPolicy.BLOCK);

        HttpResponse<String> response =
                post("/v1/privacy/redact", "{\"text\":\"key=api_testkey_abcdefghijklmnopqr\",\"policy\":\"BLOCK\"}");
        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.body()).contains("blocked");
        assertThat(response.body()).doesNotContain("api_testkey_abcdefghijklmnopqr");
    }

    @Test
    void negative_assistBlockReturns422() throws Exception {
        restartWith(new OfflineRuleBasedAiProvider(), RedactionPolicy.BLOCK);
        HttpResponse<String> response =
                post("/v1/assist", "{\"text\":\"key=api_testkey_abcdefghijklmnopqr\",\"action\":\"REWRITE\"}");
        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.body()).contains("blocked");
    }

    @Test
    void negative_assistProviderFailureReturns502() throws Exception {
        restartWith(throwingProvider(new AiProviderException("down")), RedactionPolicy.REDACT);
        HttpResponse<String> response = post("/v1/assist", "{\"text\":\"Hello\",\"action\":\"REWRITE\"}");
        assertThat(response.statusCode()).isEqualTo(502);
        assertThat(response.body()).contains("AI provider unavailable");
    }

    @Test
    void negative_assistWritingExceptionReturns500() throws Exception {
        restartWith(
                throwingProvider(new UnexpectedWritingException("boom", new IllegalStateException("x"))),
                RedactionPolicy.REDACT);
        HttpResponse<String> response = post("/v1/assist", "{\"text\":\"Hello\",\"action\":\"REWRITE\"}");
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).contains("Writing assistance failed");
    }

    @Test
    void negative_assistUnhandledRuntimeReturns500() throws Exception {
        restartWith(throwingProvider(new IllegalStateException("surprise")), RedactionPolicy.REDACT);
        HttpResponse<String> response = post("/v1/assist", "{\"text\":\"Hello\",\"action\":\"REWRITE\"}");
        assertThat(response.statusCode()).isEqualTo(500);
        // AssistWritingUseCase wraps unexpected runtime failures as UnexpectedWritingException.
        assertThat(response.body()).contains("Writing assistance failed");
    }

    @Test
    void negative_assistTruncatesLongValidationMessage() throws Exception {
        restartWith(throwingProvider(new ValidationException("x".repeat(250))), RedactionPolicy.REDACT);
        HttpResponse<String> response = post("/v1/assist", "{\"text\":\"Hello\",\"action\":\"REWRITE\"}");
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"error\"");
        assertThat(response.body().length()).isLessThan(280);
    }

    @Test
    void negative_assistBlankValidationMessageBecomesGeneric() throws Exception {
        restartWith(throwingProvider(new ValidationException("   ")), RedactionPolicy.REDACT);
        HttpResponse<String> response = post("/v1/assist", "{\"text\":\"Hello\",\"action\":\"REWRITE\"}");
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("Invalid request");
    }

    @Test
    void negative_bodyTooLargeReturns400() throws Exception {
        String huge = "a".repeat(WritingConstraints.MAX_TEXT_CHARS * 4 + 8);
        HttpResponse<String> response = post("/v1/assist", "{\"text\":\"" + huge + "\",\"action\":\"REWRITE\"}");
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("too large");
    }

    private void startDefaultServer() throws Exception {
        restartWith(new OfflineRuleBasedAiProvider(), RedactionPolicy.REDACT);
    }

    private void restartWith(AiProviderPort provider, RedactionPolicy policy) throws Exception {
        if (server != null) {
            server.close();
        }
        WritingMetrics metrics = new WritingMetrics();
        PrivacyGuard privacyGuard = new PrivacyGuard(new SensitiveTextDetector(), policy);
        AssistWritingUseCase useCase = new AssistWritingUseCase(provider, metrics, privacyGuard);
        server = new LocalWritingHttpServer(useCase, privacyGuard, metrics, findFreePort());
        server.start();
    }

    private static AiProviderPort throwingProvider(RuntimeException error) {
        return new AiProviderPort() {
            @Override
            public AiCompletionResult complete(AiCompletionCommand command) {
                throw error;
            }

            @Override
            public String providerId() {
                return "boom";
            }
        };
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path))
                        .timeout(Duration.ofSeconds(30))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static int findFreePort() throws Exception {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
