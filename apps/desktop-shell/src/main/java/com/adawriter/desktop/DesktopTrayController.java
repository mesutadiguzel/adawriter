package com.adawriter.desktop;

import com.adawriter.writing.domain.WritingAction;
import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.TrayIcon;
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
    private TrayIcon trayIcon;

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
            return false;
        }
        try {
            PopupMenu menu = new PopupMenu();
            menu.add(actionItem("Rewrite clipboard", WritingAction.REWRITE));
            menu.add(actionItem("Shorten clipboard", WritingAction.SHORTEN));
            menu.add(actionItem("Expand clipboard", WritingAction.EXPAND));
            menu.add(actionItem("Fix grammar", WritingAction.FIX_GRAMMAR));
            menu.addSeparator();
            MenuItem exit = new MenuItem("Exit AdaWriter");
            exit.addActionListener(e -> onExit.run());
            menu.add(exit);

            trayIcon = new TrayIcon(trayGateway.createTrayImage(), "AdaWriter", menu);
            trayIcon.setImageAutoSize(true);
            trayGateway.add(trayIcon);
            log.info("desktop_tray_started");
            return true;
        } catch (AWTException ex) {
            log.warn("desktop_tray_failed reason={}", ex.toString());
            return false;
        }
    }

    public void stop() {
        if (trayIcon != null && trayGateway.isSupported()) {
            trayGateway.remove(trayIcon);
            trayIcon = null;
            log.info("desktop_tray_stopped");
        }
    }

    /** Package-visible for tests: execute a clipboard assist action as the tray menu would. */
    void runAssist(WritingAction action) {
        try {
            clipboardAssist.assistFromClipboard(action, clipboardGateway::readText, clipboardGateway::writeText);
            if (trayIcon != null) {
                trayGateway.displayMessage(
                        trayIcon, "AdaWriter", action.name() + " applied to clipboard", TrayIcon.MessageType.INFO);
            }
        } catch (RuntimeException ex) {
            log.warn("clipboard_assist_failed action={} reason={}", action, ex.getMessage());
            if (trayIcon != null) {
                trayGateway.displayMessage(trayIcon, "AdaWriter", ex.getMessage(), TrayIcon.MessageType.ERROR);
            }
        }
    }

    private MenuItem actionItem(String label, WritingAction action) {
        MenuItem item = new MenuItem(label);
        item.addActionListener(e -> runAssist(action));
        return item;
    }
}
