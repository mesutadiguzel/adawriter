package com.adawriter.desktop;

import com.adawriter.writing.domain.WritingAction;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
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
    private TrayIcon trayIcon;

    public DesktopTrayController(ClipboardAssistService clipboardAssist, Runnable onExit) {
        this.clipboardAssist = Objects.requireNonNull(clipboardAssist, "clipboardAssist");
        this.onExit = Objects.requireNonNull(onExit, "onExit");
    }

    public boolean start() {
        if (!SystemTray.isSupported()) {
            log.warn("system_tray_unsupported continuing_with_agent_api_only");
            return false;
        }
        try {
            SystemTray tray = SystemTray.getSystemTray();
            PopupMenu menu = new PopupMenu();
            menu.add(actionItem("Rewrite clipboard", WritingAction.REWRITE));
            menu.add(actionItem("Shorten clipboard", WritingAction.SHORTEN));
            menu.add(actionItem("Expand clipboard", WritingAction.EXPAND));
            menu.add(actionItem("Fix grammar", WritingAction.FIX_GRAMMAR));
            menu.addSeparator();
            MenuItem exit = new MenuItem("Exit AdaWriter");
            exit.addActionListener(e -> onExit.run());
            menu.add(exit);

            Image image = Toolkit.getDefaultToolkit().createImage(new byte[] {
                /* minimal 1x1 transparent GIF */
                71,
                73,
                70,
                56,
                57,
                97,
                1,
                0,
                1,
                0,
                (byte) 128,
                0,
                0,
                0,
                0,
                0,
                (byte) 255,
                (byte) 255,
                (byte) 255,
                33,
                (byte) 249,
                4,
                1,
                0,
                0,
                0,
                0,
                44,
                0,
                0,
                0,
                0,
                1,
                0,
                1,
                0,
                0,
                2,
                1,
                68,
                0,
                59
            });
            trayIcon = new TrayIcon(image, "AdaWriter", menu);
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
            log.info("desktop_tray_started");
            return true;
        } catch (AWTException ex) {
            log.warn("desktop_tray_failed reason={}", ex.toString());
            return false;
        }
    }

    public void stop() {
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
            log.info("desktop_tray_stopped");
        }
    }

    private MenuItem actionItem(String label, WritingAction action) {
        MenuItem item = new MenuItem(label);
        item.addActionListener(e -> runAssist(action));
        return item;
    }

    private void runAssist(WritingAction action) {
        try {
            clipboardAssist.assistFromClipboard(action, this::readClipboard, this::writeClipboard);
            if (trayIcon != null) {
                trayIcon.displayMessage(
                        "AdaWriter", action.name() + " applied to clipboard", TrayIcon.MessageType.INFO);
            }
        } catch (RuntimeException ex) {
            log.warn("clipboard_assist_failed action={} reason={}", action, ex.getMessage());
            if (trayIcon != null) {
                trayIcon.displayMessage("AdaWriter", ex.getMessage(), TrayIcon.MessageType.ERROR);
            }
        }
    }

    private String readClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                return "";
            }
            return (String) clipboard.getData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | IOException ex) {
            throw new IllegalStateException("Unable to read clipboard", ex);
        }
    }

    private void writeClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
    }
}
