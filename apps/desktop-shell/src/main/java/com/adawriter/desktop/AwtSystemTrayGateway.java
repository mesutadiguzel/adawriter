package com.adawriter.desktop;

import com.adawriter.writing.domain.WritingAction;
import java.awt.AWTException;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default AWT-backed system tray gateway.
 */
public final class AwtSystemTrayGateway implements SystemTrayGateway {

    private static final Logger log = LoggerFactory.getLogger(AwtSystemTrayGateway.class);

    private TrayIcon trayIcon;

    @Override
    public boolean isSupported() {
        try {
            return SystemTray.isSupported();
        } catch (HeadlessException ex) {
            return false;
        }
    }

    @Override
    public boolean install(Consumer<WritingAction> onAction, Runnable onExit) {
        if (!isSupported()) {
            return false;
        }
        try {
            PopupMenu menu = new PopupMenu();
            menu.add(actionItem("Rewrite clipboard", WritingAction.REWRITE, onAction));
            menu.add(actionItem("Shorten clipboard", WritingAction.SHORTEN, onAction));
            menu.add(actionItem("Expand clipboard", WritingAction.EXPAND, onAction));
            menu.add(actionItem("Fix grammar", WritingAction.FIX_GRAMMAR, onAction));
            menu.addSeparator();
            MenuItem exit = new MenuItem("Exit AdaWriter");
            exit.addActionListener(e -> onExit.run());
            menu.add(exit);

            trayIcon = new TrayIcon(createTrayImage(), "AdaWriter", menu);
            trayIcon.setImageAutoSize(true);
            SystemTray.getSystemTray().add(trayIcon);
            return true;
        } catch (AWTException | HeadlessException ex) {
            log.warn("desktop_tray_install_failed reason={}", ex.toString());
            trayIcon = null;
            return false;
        }
    }

    @Override
    public void uninstall() {
        if (trayIcon != null && isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
    }

    @Override
    public void notifyInfo(String caption, String text) {
        if (trayIcon != null) {
            trayIcon.displayMessage(caption, text, TrayIcon.MessageType.INFO);
        }
    }

    @Override
    public void notifyError(String caption, String text) {
        if (trayIcon != null) {
            trayIcon.displayMessage(caption, text, TrayIcon.MessageType.ERROR);
        }
    }

    private static MenuItem actionItem(String label, WritingAction action, Consumer<WritingAction> onAction) {
        MenuItem item = new MenuItem(label);
        item.addActionListener(e -> onAction.accept(action));
        return item;
    }

    private static Image createTrayImage() {
        return Toolkit.getDefaultToolkit().createImage(new byte[] {
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
    }
}
