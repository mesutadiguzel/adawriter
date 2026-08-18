package com.adawriter.writing.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;

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
        AssistWritingUseCase useCase = new AssistWritingUseCase(new OfflineRuleBasedAiProvider(), metrics);
        server = new LocalWritingHttpServer(useCase, metrics, findFreePort());
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void healthIsUp() throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/health"))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    void assistRewriteReturnsJson() throws Exception {
        String payload = """
                {"text":"Hello from AdaWriter.","action":"REWRITE"}
                """;
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/v1/assist"))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("outputText");
        assertThat(response.body()).contains("offline");
    }

    private static int findFreePort() throws Exception {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
