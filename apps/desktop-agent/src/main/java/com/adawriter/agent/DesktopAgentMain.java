package com.adawriter.agent;

import com.adawriter.writing.adapter.ai.AiProviderFactory;
import com.adawriter.writing.adapter.rest.LocalWritingHttpServer;
import com.adawriter.writing.application.AssistWritingUseCase;
import com.adawriter.writing.application.WritingMetrics;
import com.adawriter.writing.domain.AiProviderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Composition root for the local desktop agent.
 */
public final class DesktopAgentMain {

    private static final Logger log = LoggerFactory.getLogger(DesktopAgentMain.class);

    private DesktopAgentMain() {}

    public static void main(String[] args) throws Exception {
        int port = parsePort(env("ADAWRITER_PORT", "8787"));
        AiProviderPort aiProvider = AiProviderFactory.fromEnvironment();
        WritingMetrics metrics = new WritingMetrics();
        AssistWritingUseCase useCase = new AssistWritingUseCase(aiProvider, metrics);

        try (LocalWritingHttpServer server = new LocalWritingHttpServer(useCase, metrics, port)) {
            server.start();
            log.info(
                    "desktop_agent_ready port={} provider={} tip=POST /v1/assist health=GET /health",
                    port,
                    aiProvider.providerId());

            Thread keepAlive = Thread.ofVirtual()
                    .name("desktop-agent-keepalive")
                    .start(() -> {
                        try {
                            Thread.sleep(Long.MAX_VALUE);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }
                    });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("desktop_agent_shutting_down");
                keepAlive.interrupt();
            }));

            keepAlive.join();
        }
    }

    private static int parsePort(String raw) {
        try {
            int port = Integer.parseInt(raw);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("ADAWRITER_PORT out of range: " + raw);
            }
            return port;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid ADAWRITER_PORT: " + raw, ex);
        }
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
