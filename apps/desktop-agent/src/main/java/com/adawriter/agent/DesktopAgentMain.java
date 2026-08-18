package com.adawriter.agent;

import com.adawriter.privacy.application.PrivacyGuard;
import com.adawriter.privacy.domain.RedactionPolicy;
import com.adawriter.privacy.domain.SensitiveTextDetector;
import com.adawriter.writing.adapter.ai.AiProviderFactory;
import com.adawriter.writing.adapter.rest.LocalWritingHttpServer;
import com.adawriter.writing.application.AssistWritingUseCase;
import com.adawriter.writing.application.WritingMetrics;
import com.adawriter.writing.domain.AiProviderPort;
import java.util.Locale;
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
        PrivacyGuard privacyGuard = new PrivacyGuard(new SensitiveTextDetector(), parseAssistPolicy());
        AssistWritingUseCase useCase = new AssistWritingUseCase(aiProvider, metrics, privacyGuard);

        try (LocalWritingHttpServer server = new LocalWritingHttpServer(useCase, privacyGuard, metrics, port)) {
            server.start();
            log.info(
                    "desktop_agent_ready port={} provider={} privacyPolicy={} tip=POST /v1/assist|/v1/privacy/detect",
                    port,
                    aiProvider.providerId(),
                    privacyGuard.defaultAssistPolicy());

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

    private static RedactionPolicy parseAssistPolicy() {
        String raw = env("ADAWRITER_PRIVACY_POLICY", "REDACT").trim().toUpperCase(Locale.ROOT);
        try {
            RedactionPolicy policy = RedactionPolicy.valueOf(raw);
            if (policy == RedactionPolicy.REPORT_ONLY) {
                throw new IllegalArgumentException("ADAWRITER_PRIVACY_POLICY for assist must be REDACT or BLOCK");
            }
            return policy;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid ADAWRITER_PRIVACY_POLICY: " + raw, ex);
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
