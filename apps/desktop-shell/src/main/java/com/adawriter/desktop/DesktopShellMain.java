package com.adawriter.desktop;

import com.adawriter.privacy.application.PrivacyGuard;
import com.adawriter.privacy.domain.RedactionPolicy;
import com.adawriter.privacy.domain.SensitiveTextDetector;
import com.adawriter.writing.adapter.ai.AiProviderFactory;
import com.adawriter.writing.adapter.rest.LocalWritingHttpServer;
import com.adawriter.writing.application.AssistWritingUseCase;
import com.adawriter.writing.application.WritingMetrics;
import com.adawriter.writing.domain.AiProviderPort;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Desktop shell composition root: local agent API + optional system tray.
 */
public final class DesktopShellMain {

    private static final Logger log = LoggerFactory.getLogger(DesktopShellMain.class);

    private DesktopShellMain() {}

    public static void main(String[] args) throws Exception {
        int port = parsePort(env("ADAWRITER_PORT", "8787"));
        AiProviderPort aiProvider = AiProviderFactory.fromEnvironment();
        WritingMetrics metrics = new WritingMetrics();
        PrivacyGuard privacyGuard = new PrivacyGuard(new SensitiveTextDetector(), parseAssistPolicy());
        AssistWritingUseCase useCase = new AssistWritingUseCase(aiProvider, metrics, privacyGuard);
        ClipboardAssistService clipboardAssist = new ClipboardAssistService(useCase);

        CountDownLatch shutdown = new CountDownLatch(1);
        DesktopTrayController tray = new DesktopTrayController(clipboardAssist, shutdown::countDown);

        try (LocalWritingHttpServer server = new LocalWritingHttpServer(useCase, privacyGuard, metrics, port)) {
            server.start();
            boolean trayStarted = tray.start();
            log.info(
                    "desktop_shell_ready port={} provider={} privacyPolicy={} tray={}",
                    port,
                    aiProvider.providerId(),
                    privacyGuard.defaultAssistPolicy(),
                    trayStarted);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("desktop_shell_shutting_down");
                tray.stop();
                shutdown.countDown();
            }));

            shutdown.await();
            tray.stop();
        }
    }

    private static RedactionPolicy parseAssistPolicy() {
        String raw = env("ADAWRITER_PRIVACY_POLICY", "REDACT").trim().toUpperCase(Locale.ROOT);
        RedactionPolicy policy = RedactionPolicy.valueOf(raw);
        if (policy == RedactionPolicy.REPORT_ONLY) {
            throw new IllegalArgumentException("ADAWRITER_PRIVACY_POLICY for assist must be REDACT or BLOCK");
        }
        return policy;
    }

    private static int parsePort(String raw) {
        int port = Integer.parseInt(raw);
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("ADAWRITER_PORT out of range: " + raw);
        }
        return port;
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
