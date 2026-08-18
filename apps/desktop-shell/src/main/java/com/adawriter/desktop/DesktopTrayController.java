package com.adawriter.desktop;

import com.adawriter.writing.domain.WritingAction;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * System tray UI for AdaWriter desktop shell.
 */
public final class DesktopTrayController {

    private static final Logger log = LoggerFactory.getLogger(DesktopTrayController.class);

    private final ClipboardAssistService clipboardAssist;
    private final Runnable onExit;
    private final SystemTrayGateway trayGateway;
    private final ClipboardGateway clipboardGateway;
    private boolean started;

    public DesktopTrayController(ClipboardAssistService clipboardAssist, Runnable onExit) {
        this(clipboardAssist, onExit, new AwtSystemTrayGateway(), new AwtClipboardGateway());
    }

    public DesktopTrayController(
            ClipboardAssistService clipboardAssist,
            Runnable onExit,
            SystemTrayGateway trayGateway,
            ClipboardGateway clipboardGateway) {
        this.clipboardAssist = Objects.requireNonNull(clipboardAssist, "clipboardAssist");
        this.onExit = Objects.requireNonNull(onExit, "onExit");
        this.trayGateway = Objects.requireNonNull(trayGateway, "trayGateway");
        this.clipboardGateway = Objects.requireNonNull(clipboardGateway, "clipboardGateway");
    }

    public boolean start() {
        if (!trayGateway.isSupported()) {
            log.warn("system_tray_unsupported continuing_with_agent_api_only");
            started = false;
            return false;
        }
        boolean installed = trayGateway.install(this::runAssist, onExit);
        if (!installed) {
            log.warn("desktop_tray_failed");
            started = false;
            return false;
        }
        started = true;
        log.info("desktop_tray_started");
        return true;
    }

    public void stop() {
        if (started) {
            trayGateway.uninstall();
            started = false;
            log.info("desktop_tray_stopped");
        }
    }

    /** Package-visible for tests: execute a clipboard assist action as the tray menu would. */
    void runAssist(WritingAction action) {
        try {
            clipboardAssist.assistFromClipboard(action, clipboardGateway::readText, clipboardGateway::writeText);
            trayGateway.notifyInfo("AdaWriter", action.name() + " applied to clipboard");
        } catch (RuntimeException ex) {
            log.warn("clipboard_assist_failed action={} reason={}", action, ex.getMessage());
            trayGateway.notifyError("AdaWriter", ex.getMessage());
        }
    }
}
