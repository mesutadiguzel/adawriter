package com.adawriter.desktop;

import com.adawriter.writing.domain.WritingAction;
import java.util.function.Consumer;

/**
 * Abstraction over system tray install/notify so controller logic is headless-testable.
 */
public interface SystemTrayGateway {

    boolean isSupported();

    /**
     * Installs the tray UI. Invokes {@code onAction} for assist menu items and {@code onExit} for exit.
     *
     * @return true when tray UI was installed
     */
    boolean install(Consumer<WritingAction> onAction, Runnable onExit);

    void uninstall();

    void notifyInfo(String caption, String text);

    void notifyError(String caption, String text);
}
