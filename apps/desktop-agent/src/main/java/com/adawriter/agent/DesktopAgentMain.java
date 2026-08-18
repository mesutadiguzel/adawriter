package com.adawriter.agent;

import com.adawriter.privacy.application.PrivacyGuard;
import com.adawriter.privacy.domain.SensitiveTextDetector;
import com.adawriter.writing.adapter.ai.AiProviderFactory;
import com.adawriter.writing.adapter.rest.LocalWritingHttpServer;
import com.adawriter.writing.application.AssistWritingUseCase;
import com.adawriter.writing.application.DesktopRuntimeConfig;
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
        DesktopRuntimeConfig config = DesktopRuntimeConfig.fromEnvironment();
        AiProviderPort aiProvider = AiProviderFactory.fromEnvironment();
        WritingMetrics metrics = new WritingMetrics();
        PrivacyGuard privacyGuard = new PrivacyGuard(new SensitiveTextDetector(), config.assistPrivacyPolicy());
        AssistWritingUseCase useCase = new AssistWritingUseCase(aiProvider, metrics, privacyGuard);

        try (LocalWritingHttpServer server =
                new LocalWritingHttpServer(useCase, privacyGuard, metrics, config.port())) {
            server.start();
            log.info(
                    "desktop_agent_ready port={} provider={} privacyPolicy={} tip=POST /v1/assist|/v1/privacy/detect",
                    config.port(),
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
}
