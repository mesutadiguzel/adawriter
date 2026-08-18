package com.adawriter.desktop;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.TrayIcon;

/**
 * Abstraction over AWT system tray so tray controller logic is unit-testable.
 */
public interface SystemTrayGateway {

    boolean isSupported();

    Image createTrayImage();

    void add(TrayIcon icon) throws AWTException;

    void remove(TrayIcon icon);

    void displayMessage(TrayIcon icon, String caption, String text, TrayIcon.MessageType type);
}
