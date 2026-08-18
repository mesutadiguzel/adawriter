package com.adawriter.writing.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.adawriter.privacy.application.PrivacyGuard;
import com.adawriter.privacy.domain.RedactionPolicy;
import com.adawriter.privacy.domain.SensitiveTextDetector;
import com.adawriter.writing.adapter.ai.OfflineRuleBasedAiProvider;
import com.adawriter.writing.application.AssistWritingUseCase;
import com.adawriter.writing.application.WritingMetrics;
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
        WritingMetrics metrics = new WritingMetrics();
        PrivacyGuard privacyGuard = PrivacyGuard.withDefaults();
        AssistWritingUseCase useCase =
                new AssistWritingUseCase(new OfflineRuleBasedAiProvider(), metrics, privacyGuard);
        server = new LocalWritingHttpServer(useCase, privacyGuard, metrics, findFreePort());
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
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
    }

    @Test
    void positive_assistRewriteReturnsJson() throws Exception {
        HttpResponse<String> response =
                post("/v1/assist", "{\"text\":\"Hello from AdaWriter.\",\"action\":\"REWRITE\"}");
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
    void negative_assistRejectsWrongMethod() throws Exception {
        HttpResponse<String> response = get("/v1/assist");
        assertThat(response.statusCode()).isEqualTo(405);
    }

    @Test
    void negative_detectRejectsBlankText() throws Exception {
        HttpResponse<String> response = post("/v1/privacy/detect", "{\"text\":\"   \"}");
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void negative_redactBlockReturns422() throws Exception {
        tearDown();
        WritingMetrics metrics = new WritingMetrics();
        PrivacyGuard blocking = new PrivacyGuard(new SensitiveTextDetector(), RedactionPolicy.BLOCK);
        AssistWritingUseCase useCase = new AssistWritingUseCase(new OfflineRuleBasedAiProvider(), metrics, blocking);
        server = new LocalWritingHttpServer(useCase, blocking, metrics, findFreePort());
        server.start();

        HttpResponse<String> response =
                post("/v1/privacy/redact", "{\"text\":\"key=api_testkey_abcdefghijklmnopqr\",\"policy\":\"BLOCK\"}");
        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.body()).contains("blocked");
        assertThat(response.body()).doesNotContain("api_testkey_abcdefghijklmnopqr");
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
                        .timeout(Duration.ofSeconds(5))
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
