package com.adawriter.writing.adapter.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.adawriter.writing.domain.AiCompletionCommand;
import com.adawriter.writing.domain.AiCompletionResult;
import com.adawriter.writing.domain.AiProviderException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleAiProviderTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void positive_parsesChatCompletion() throws Exception {
        AtomicReference<String> auth = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body =
                    """
                    {"model":"mock-model","choices":[{"message":{"content":"Hello from cloud"}}]}
                    """
                            .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        OpenAiCompatibleAiProvider provider = new OpenAiCompatibleAiProvider(
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions"),
                "test-key",
                "gpt-test",
                Duration.ofSeconds(5));

        AiCompletionResult result = provider.complete(new AiCompletionCommand("sys", "user", 64));
        assertThat(result.text()).isEqualTo("Hello from cloud");
        assertThat(result.modelId()).isEqualTo("mock-model");
        assertThat(auth.get()).isEqualTo("Bearer test-key");
        assertThat(provider.providerId()).isEqualTo(OpenAiCompatibleAiProvider.PROVIDER_ID);
    }

    @Test
    void negative_httpErrorBecomesProviderException() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] body = "{\"error\":\"nope\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        OpenAiCompatibleAiProvider provider = new OpenAiCompatibleAiProvider(
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions"),
                "",
                "gpt-test",
                Duration.ofSeconds(5));

        assertThatThrownBy(() -> provider.complete(new AiCompletionCommand("sys", "user", 64)))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("HTTP 500");
    }

    @Test
    void negative_missingContentFails() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] body = "{\"choices\":[{\"message\":{}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        OpenAiCompatibleAiProvider provider = new OpenAiCompatibleAiProvider(
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions"),
                "",
                "gpt-test",
                Duration.ofSeconds(5));

        assertThatThrownBy(() -> provider.complete(new AiCompletionCommand("sys", "user", 64)))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("missing content");
    }

    @Test
    void negative_ioFailureBecomesProviderException() throws Exception {
        HttpClient failing = new HttpClient() {
            @Override
            public Optional<Authenticator> authenticator() {
                return Optional.empty();
            }

            @Override
            public Optional<CookieHandler> cookieHandler() {
                return Optional.empty();
            }

            @Override
            public Optional<Duration> connectTimeout() {
                return Optional.empty();
            }

            @Override
            public HttpClient.Redirect followRedirects() {
                return Redirect.NEVER;
            }

            @Override
            public Optional<ProxySelector> proxy() {
                return Optional.empty();
            }

            @Override
            public SSLContext sslContext() {
                throw new UnsupportedOperationException();
            }

            @Override
            public SSLParameters sslParameters() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<Executor> executor() {
                return Optional.empty();
            }

            @Override
            public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                    throws IOException {
                throw new IOException("network down");
            }

            @Override
            public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                    HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                    HttpRequest request,
                    HttpResponse.BodyHandler<T> responseBodyHandler,
                    HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
                throw new UnsupportedOperationException();
            }

            @Override
            public HttpClient.Version version() {
                return Version.HTTP_1_1;
            }
        };

        OpenAiCompatibleAiProvider provider = new OpenAiCompatibleAiProvider(
                failing, URI.create("http://127.0.0.1:9/v1/chat/completions"), "", "gpt-test", Duration.ofSeconds(1));

        assertThatThrownBy(() -> provider.complete(new AiCompletionCommand("sys", "user", 64)))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("I/O");
    }
}
