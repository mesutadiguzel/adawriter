package com.adawriter.desktop;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;

/**
 * Default AWT-backed system tray gateway.
 */
public final class AwtSystemTrayGateway implements SystemTrayGateway {

    @Override
    public boolean isSupported() {
        return SystemTray.isSupported();
    }

    @Override
    public Image createTrayImage() {
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

    @Override
    public void add(TrayIcon icon) throws AWTException {
        SystemTray.getSystemTray().add(icon);
    }

    @Override
    public void remove(TrayIcon icon) {
        SystemTray.getSystemTray().remove(icon);
    }

    @Override
    public void displayMessage(TrayIcon icon, String caption, String text, TrayIcon.MessageType type) {
        icon.displayMessage(caption, text, type);
    }
}
